const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions, logger } = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();
setGlobalOptions({ region: "asia-southeast1", maxInstances: 10 });

exports.dispatchTenantNotificationPush = onDocumentCreated(
  {
    document: "tenants/{tenantId}/notifications/{notificationId}",
    retry: true
  },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      return;
    }

    const notification = snapshot.data();
    const tenantId = event.params.tenantId;
    const notificationId = event.params.notificationId;

    const recipientUid = (notification && notification.userId ? String(notification.userId) : "").trim();
    if (!recipientUid) {
      await markPushState(snapshot.ref, "FAILED", "missing_user_id");
      return;
    }

    const tokenCollection = admin
      .firestore()
      .collection("users")
      .doc(recipientUid)
      .collection("fcm_tokens");

    const tokenSnap = await tokenCollection.get();
    const tokens = tokenSnap.docs
      .map((doc) => {
        const value = doc.get("token");
        return typeof value === "string" ? value.trim() : "";
      })
      .filter((token) => token.length > 0);

    if (tokens.length === 0) {
      await markPushState(snapshot.ref, "NO_TOKEN", "recipient_has_no_tokens");
      return;
    }

    const title = normalizeText(notification.title, "MOPR");
    const body = normalizeText(notification.body, "");
    const type = normalizeText(notification.type, "CHAT_MESSAGE");
    const conversationId = normalizeText(notification.conversationId, "");

    const message = {
      notification: {
        title,
        body
      },
      data: {
        tenantId,
        notificationId,
        type,
        conversationId
      },
      android: {
        priority: "high"
      },
      tokens
    };

    const response = await admin.messaging().sendEachForMulticast(message);
    const successCount = response.successCount || 0;
    const failureCount = response.failureCount || 0;

    if (successCount > 0) {
      await markPushState(snapshot.ref, failureCount > 0 ? "PARTIAL" : "SENT", "");
    } else {
      const firstError = response.responses.find((r) => !r.success && r.error);
      const errorMessage = firstError && firstError.error ? String(firstError.error.message || "") : "send_failed";
      await markPushState(snapshot.ref, "FAILED", errorMessage);
    }

    if (failureCount > 0) {
      const invalidTokens = [];
      response.responses.forEach((res, index) => {
        if (!res.success && res.error && isTokenInvalidError(res.error.code)) {
          invalidTokens.push(tokens[index]);
        }
      });
      await deleteInvalidTokens(tokenCollection, invalidTokens);
    }

    logger.info("dispatchTenantNotificationPush", {
      tenantId,
      notificationId,
      recipientUid,
      tokenCount: tokens.length,
      successCount,
      failureCount
    });
  }
);

exports.processContractHardOffboard = onDocumentCreated(
  {
    document: "tenants/{tenantId}/auditLogs/{logId}",
    retry: true
  },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      return;
    }

    const tenantId = event.params.tenantId;
    const logId = event.params.logId;
    const payload = snapshot.data() || {};
    if (String(payload.type || "").trim() !== "CONTRACT_HARD_OFFBOARD") {
      return;
    }

    const affectedUserIds = normalizeStringArray(payload.affectedUserIds);
    const contractId = normalizeText(payload.contractId, "");
    const roomId = normalizeText(payload.roomId, "");

    const db = admin.firestore();
    const batch = db.batch();
    const now = admin.firestore.FieldValue.serverTimestamp();
    const updatedUsers = [];

    for (const uid of affectedUserIds) {
      const userRef = db.collection("users").doc(uid);
      const userSnap = await userRef.get();
      if (!userSnap.exists) {
        continue;
      }

      const activeTenantId = normalizeText(userSnap.get("activeTenantId"), "");
      if (activeTenantId !== tenantId) {
        continue;
      }

      batch.set(
        userRef,
        {
          activeTenantId: null,
          activeContractMemberRole: null,
          updatedAt: now
        },
        { merge: true }
      );

      const notificationId = `contract_end_relink_${logId}_${uid}`;
      const notifRef = db
        .collection("tenants")
        .doc(tenantId)
        .collection("notifications")
        .doc(notificationId);

      batch.set(
        notifRef,
        {
          userId: uid,
          title: "Hop dong da ket thuc",
          body: "Hop dong phong cua ban da ket thuc. Vui long nhap ma phong moi de tiep tuc su dung.",
          type: "CONTRACT_ENDED_RELINK",
          contractId,
          roomId,
          senderId: null,
          conversationId: null,
          isRead: false,
          createdAt: now
        },
        { merge: true }
      );

      updatedUsers.push(uid);
    }

    batch.set(
      snapshot.ref,
      {
        processingState: "DONE",
        processedAt: now,
        affectedCount: updatedUsers.length
      },
      { merge: true }
    );

    await batch.commit();

    logger.info("processContractHardOffboard", {
      tenantId,
      logId,
      contractId,
      roomId,
      affectedCount: updatedUsers.length
    });
  }
);

async function markPushState(ref, state, reason) {
  const payload = {
    pushState: state,
    pushUpdatedAt: admin.firestore.FieldValue.serverTimestamp()
  };
  if (reason) {
    payload.pushError = reason;
  }
  await ref.set(payload, { merge: true });
}

async function deleteInvalidTokens(tokenCollection, invalidTokens) {
  if (!Array.isArray(invalidTokens) || invalidTokens.length === 0) {
    return;
  }

  const tokenSet = new Set(invalidTokens);
  const tokenSnap = await tokenCollection.get();
  const batch = admin.firestore().batch();

  tokenSnap.docs.forEach((doc) => {
    const token = doc.get("token");
    if (typeof token === "string" && tokenSet.has(token.trim())) {
      batch.delete(doc.ref);
    }
  });

  await batch.commit();
}

function normalizeText(value, fallback) {
  if (typeof value !== "string") {
    return fallback;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : fallback;
}

function isTokenInvalidError(code) {
  return code === "messaging/invalid-registration-token" || code === "messaging/registration-token-not-registered";
}

function normalizeStringArray(value) {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => (typeof item === "string" ? item.trim() : ""))
    .filter((item) => item.length > 0);
}
