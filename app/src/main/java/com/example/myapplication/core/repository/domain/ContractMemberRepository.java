package com.example.myapplication.core.repository.domain;

import com.example.myapplication.core.session.TenantSession;
import com.example.myapplication.domain.ContractMember;
import com.example.myapplication.domain.Tenant;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ContractMemberRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION = "contractMembers";

    private CollectionReference scopedCollection() {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(COLLECTION);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("User not logged in");
        }
        return db.collection("users").document(user.getUid()).collection(COLLECTION);
    }

    public void upsertPrimaryMemberFromContract(Tenant contract, Runnable onSuccess, Runnable onFail) {
        if (contract == null || contract.getId() == null || contract.getId().trim().isEmpty()) {
            if (onFail != null) {
                onFail.run();
            }
            return;
        }

        long now = System.currentTimeMillis();
        CollectionReference col = scopedCollection();
        col.whereEqualTo("contractId", contract.getId())
                .whereEqualTo("primaryContact", true)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    ContractMember member = mapPrimaryMember(contract, now);
                    if (qs != null && !qs.isEmpty()) {
                        DocumentSnapshot doc = qs.getDocuments().get(0);
                        member.setCreatedAt(doc.getLong("createdAt") != null ? doc.getLong("createdAt") : now);
                        col.document(doc.getId()).set(member)
                                .addOnSuccessListener(v -> {
                                    if (onSuccess != null) {
                                        onSuccess.run();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (onFail != null) {
                                        onFail.run();
                                    }
                                });
                    } else {
                        col.add(member)
                                .addOnSuccessListener(v -> {
                                    if (onSuccess != null) {
                                        onSuccess.run();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (onFail != null) {
                                        onFail.run();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (onFail != null) {
                        onFail.run();
                    }
                });
    }

    public void deactivateMembersByContract(String contractId, Runnable onDone) {
        if (contractId == null || contractId.trim().isEmpty()) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }

        CollectionReference col = scopedCollection();
        col.whereEqualTo("contractId", contractId)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs == null || qs.isEmpty()) {
                        if (onDone != null) {
                            onDone.run();
                        }
                        return;
                    }

                    long now = System.currentTimeMillis();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("active", false);
                    updates.put("updatedAt", now);

                    final int total = qs.size();
                    final int[] done = { 0 };
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        col.document(doc.getId()).update(updates)
                                .addOnCompleteListener(task -> {
                                    done[0]++;
                                    if (done[0] >= total && onDone != null) {
                                        onDone.run();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (onDone != null) {
                        onDone.run();
                    }
                });
    }

    private ContractMember mapPrimaryMember(Tenant contract, long now) {
        ContractMember member = new ContractMember();
        member.setContractId(contract.getId());
        member.setRoomId(contract.getRoomId());
        member.setRoomNumber(contract.getRoomNumber());
        member.setFullName(contract.getFullName());
        member.setPersonalId(contract.getPersonalId());
        member.setPhoneNumber(contract.getPhoneNumber());
        member.setPrimaryContact(true);
        member.setContractRepresentative(true);
        member.setTemporaryResident(contract.isTemporaryResident());
        member.setFullyDocumented(contract.isFullyDocumented());
        member.setActive(!"ENDED".equalsIgnoreCase(contract.getContractStatus()));
        member.setUpdatedAt(now);
        if (member.getCreatedAt() == null) {
            member.setCreatedAt(now);
        }
        return member;
    }
}
