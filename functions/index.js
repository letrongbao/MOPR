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
