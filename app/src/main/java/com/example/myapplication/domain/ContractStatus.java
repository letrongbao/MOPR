package com.example.myapplication.domain;

public enum ContractStatus {
    DANG_THUE,    // Đang thuê (còn > 30 ngày)
    SAP_HET_HAN,  // Sắp hết hạn (còn <= 30 ngày)
    DA_KET_THUC   // Đã kết thúc (đã qua ngày hoặc ENDED)
}
