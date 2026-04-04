package com.example.myapplication.domain;

public enum ContractStatus {
    ACTIVE_RENTAL,    // Active rental (more than 30 days remaining)
    EXPIRING_SOON,  // Expiring soon (30 days or fewer remaining)
    ENDED   // Ended (past end date or marked ENDED)
}

