const fs = require('fs');

function applyChanges() {
    try {
        const revFile = 'app/src/main/java/com/example/myapplication/features/finance/RevenueActivity.java';
        let rev = fs.readFileSync(revFile, 'utf8');

        if (!rev.includes('import com.example.myapplication.domain.House;')) {
            rev = rev.replace('import com.example.myapplication.domain.Invoice;', 'import com.example.myapplication.domain.House;\nimport com.example.myapplication.domain.Invoice;');
        }
        if (!rev.includes('import android.widget.Spinner;')) {
            rev = rev.replace('import android.widget.TextView;', 'import android.widget.TextView;\nimport android.widget.Spinner;\nimport android.widget.ArrayAdapter;\nimport android.widget.AdapterView;');
        }

        const revVars = `
    private Spinner spinnerHouse;
    private List<House> allHouses = new ArrayList<>();
    private List<Room> allRooms = new ArrayList<>();
    private String selectedHouseId = null;
`;
        if (!rev.includes('private Spinner spinnerHouse;')) {
            rev = rev.replace('private String selectedMonth;', revVars + '    private String selectedMonth;');
        }

        if (!rev.includes('spinnerHouse = findViewById(R.id.spinnerHouse);')) {
            const revInitSpinner = `
        spinnerHouse = findViewById(R.id.spinnerHouse);
        scopedCollection("houses").addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            allHouses.clear();
            for (QueryDocumentSnapshot doc : snapshot) {
                House h = doc.toObject(House.class);
                if (h != null) {
                    h.setId(doc.getId());
                    allHouses.add(h);
                }
            }
            List<String> houseNames = new ArrayList<>();
            houseNames.add("Tất cả nhà");
            for (House h : allHouses) {
                houseNames.add(h.getHouseName() != null ? h.getHouseName() : "Nhà " + h.getId());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, houseNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerHouse.setAdapter(adapter);
        });

        spinnerHouse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedHouseId = null;
                } else {
                    selectedHouseId = allHouses.get(position - 1).getId();
                }
                updateReportStats(fmt);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedHouseId = null;
                updateReportStats(fmt);
            }
        });
`;
            rev = rev.replace('tvSelectedMonth = findViewById(R.id.tvSelectedMonth);', 'tvSelectedMonth = findViewById(R.id.tvSelectedMonth);\n' + revInitSpinner);
        }

        const roomsFetch = `viewModelProvider.get(RoomViewModel.class)
                .getRoomList().observe(this, list -> {
                    if (list == null)
                        return;
                    allRooms = list;
                    updateReportStats(fmt);
                });`;
        if (!rev.includes('allRooms = list;')) {
            rev = rev.replace(/viewModelProvider\.get\(RoomViewModel\.class\)[\s\S]*?tvTyLeLapDay\.setText.*?\n\s+\}\);/m, roomsFetch);
        }

        const filterLogic = `if (lastInvoices == null)
            return;

        int totalRoomCount = 0;
        int rentedRoomCount = 0;
        Map<String, String> roomToHouseMap = new HashMap<>();
        for (Room r : allRooms) {
            if (r != null && r.getId() != null) {
                roomToHouseMap.put(r.getId(), r.getHouseId());
                if (selectedHouseId == null || selectedHouseId.equals(r.getHouseId())) {
                    totalRoomCount++;
                    if (RoomStatus.RENTED.equals(r.getStatus())) {
                        rentedRoomCount++;
                    }
                }
            }
        }
        
        if (tvTongPhong != null) tvTongPhong.setText(String.valueOf(totalRoomCount));
        if (tvPhongDaThua != null) tvPhongDaThua.setText(String.valueOf(rentedRoomCount));
        if (tvTyLeLapDay != null) {
            double rate = totalRoomCount > 0 ? (100.0 * rentedRoomCount / totalRoomCount) : 0;
            tvTyLeLapDay.setText(getString(R.string.occupancy_rate_label,
                    String.format(Locale.getDefault(), "%.1f%%", rate)));
        }

        int chuaThu = 0;`;
        if (rev.includes('int chuaThu = 0;') && !rev.includes('roomToHouseMap = new HashMap') && rev.includes('if (lastInvoices == null)')) {
            rev = rev.replace(/if \(lastInvoices == null\)\s*return;\s*int chuaThu = 0;/, filterLogic);
        }

        const filterInvoice = `String roomId = h.getRoomId();
            if (roomId != null) {
                String hId = roomToHouseMap.get(roomId);
                if (selectedHouseId != null && !selectedHouseId.equals(hId)) {
                    continue;
                }
            }`;
        if (!rev.includes('String hId = roomToHouseMap.get(roomId);')) {
            rev = rev.replace('String invoiceId = h.getId();', filterInvoice + '\n            String invoiceId = h.getId();');
        }

        fs.writeFileSync(revFile, rev);

        const expFile = 'app/src/main/java/com/example/myapplication/features/finance/ExpenseActivity.java';
        let exp = fs.readFileSync(expFile, 'utf8');

        if (!exp.includes('import com.example.myapplication.domain.House;')) {
            exp = exp.replace('import com.example.myapplication.domain.Expense;', 'import com.example.myapplication.domain.House;\nimport com.example.myapplication.domain.Expense;');
        }
        if (!exp.includes('import android.widget.Spinner;')) {
            exp = exp.replace('import android.widget.TextView;', 'import android.widget.TextView;\nimport android.widget.Spinner;\nimport android.widget.ArrayAdapter;\nimport android.widget.AdapterView;');
        }
        if (!exp.includes('import com.google.firebase.firestore.QueryDocumentSnapshot;')) {
            exp = exp.replace('import com.google.firebase.firestore.FirebaseFirestore;', 'import com.google.firebase.firestore.FirebaseFirestore;\nimport com.google.firebase.firestore.QueryDocumentSnapshot;\nimport com.google.firebase.firestore.CollectionReference;');
        }

        const expVars = `
    private Spinner spinnerHouse;
    private List<House> allHouses = new ArrayList<>();
    private String selectedHouseId = null;
`;
        if (!exp.includes('private Spinner spinnerHouse;')) {
            exp = exp.replace('private String selectedMonth;', expVars + '    private String selectedMonth;');
        }

        const expScopes = `private CollectionReference scopedCollection(String path) {
        String tenantId = TenantSession.getActiveTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return db.collection("tenants").document(tenantId).collection(path);
        }
        return db.collection(path);
    }`;
        if (!exp.includes('scopedCollection(String path)')) {
            exp = exp.replace('private void ensurePermissionsThenLoad() {', expScopes + '\n\n    private void ensurePermissionsThenLoad() {');
        }

        if (!exp.includes('spinnerHouse = findViewById(R.id.spinnerHouse);')) {
            const expInitSpinner = `
        spinnerHouse = findViewById(R.id.spinnerHouse);
        scopedCollection("houses").addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            allHouses.clear();
            for (QueryDocumentSnapshot doc : snapshot) {
                House h = doc.toObject(House.class);
                if (h != null) {
                    h.setId(doc.getId());
                    allHouses.add(h);
                }
            }
            List<String> houseNames = new ArrayList<>();
            houseNames.add("Tất cả nhà");
            for (House h : allHouses) {
                houseNames.add(h.getHouseName() != null ? h.getHouseName() : "Nhà " + h.getId());
            }
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, houseNames);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerHouse.setAdapter(spinnerAdapter);
        });

        spinnerHouse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedHouseId = null;
                } else {
                    selectedHouseId = allHouses.get(position - 1).getId();
                }
                applyMonthFilterAndSummary();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedHouseId = null;
                applyMonthFilterAndSummary();
            }
        });
`;
            exp = exp.replace('tvSelectedMonth = findViewById(R.id.tvSelectedMonth);', 'tvSelectedMonth = findViewById(R.id.tvSelectedMonth);\n' + expInitSpinner);
        }

        const expFilter = `List<Expense> filtered = new ArrayList<>();
        for (Expense cp : allExpenses) {
            if (cp == null)
                continue;
            
            if (selectedHouseId != null && !selectedHouseId.equals(cp.getHouseId())) {
                continue;
            }

            String month = FinancePeriodUtil.normalizeMonthYear(cp.getPaidAt());`;
        if (!exp.includes('!selectedHouseId.equals(cp.getHouseId())')) {
            exp = exp.replace(/List<Expense> filtered = new ArrayList<>\(\);\s*for \(Expense cp : allExpenses\) \{\s*if \(cp == null\)\s*continue;\s*String month = FinancePeriodUtil\.normalizeMonthYear\(cp\.getPaidAt\(\)\);/m, expFilter);
        }

        fs.writeFileSync(expFile, exp);
        console.log("Updated both files successfully.");
    } catch (e) {
        console.error(e);
    }
}
applyChanges();
