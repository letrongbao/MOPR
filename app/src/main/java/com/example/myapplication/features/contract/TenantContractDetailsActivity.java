package com.example.myapplication.features.contract;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class TenantContractDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = "ROOM_ID";
    public static final String EXTRA_TENANT_ID = "TENANT_ID";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        String tenantId = getIntent().getStringExtra(EXTRA_TENANT_ID);
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = TenantSession.getActiveTenantId();
        }

        if (roomId == null || roomId.trim().isEmpty() || tenantId == null || tenantId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.contract_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findAndOpenContract(tenantId, roomId);
    }

    private void findAndOpenContract(String tenantId, String roomId) {
        db.collection("tenants").document(tenantId)
                .collection("contracts")
                .whereEqualTo("roomId", roomId)
                .limit(5)
                .get()
                .addOnSuccessListener(qs -> {
                    DocumentSnapshot contractDoc = findActiveContract(qs);
                    if (contractDoc != null) {
                        openContractDetails(contractDoc.getId());
                        return;
                    }
                    fallbackUserContracts(tenantId, roomId);
                })
                .addOnFailureListener(e -> fallbackUserContracts(tenantId, roomId));
    }

    private void fallbackUserContracts(String tenantId, String roomId) {
        db.collection("users").document(tenantId)
                .collection("contracts")
                .whereEqualTo("roomId", roomId)
                .limit(5)
                .get()
                .addOnSuccessListener(qs -> {
                    DocumentSnapshot contractDoc = findActiveContract(qs);
                    if (contractDoc != null) {
                        openContractDetails(contractDoc.getId());
                    } else {
                        Toast.makeText(this, getString(R.string.contract_not_found), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.contract_not_found), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private DocumentSnapshot findActiveContract(QuerySnapshot qs) {
        if (qs == null || qs.isEmpty()) {
            return null;
        }
        for (DocumentSnapshot doc : qs.getDocuments()) {
            String status = doc.getString("contractStatus");
            if ("ACTIVE".equalsIgnoreCase(status)) {
                return doc;
            }
        }
        return qs.getDocuments().get(0);
    }

    private void openContractDetails(String contractId) {
        Intent intent = new Intent(this, ContractDetailsActivity.class);
        intent.putExtra(ContractDetailsActivity.EXTRA_CONTRACT_ID, contractId);
        intent.putExtra(ContractDetailsActivity.EXTRA_HEADER_TITLE, getString(R.string.tenant_contract_your_title));
        startActivity(intent);
        finish();
    }
}
