package com.example.myapplication.features.contract;

import androidx.annotation.NonNull;

import com.example.myapplication.domain.Tenant;

import java.util.Calendar;
import java.util.Locale;

public final class ContractFormDataHelper {

    public interface MonthYearNormalizer {
        @NonNull
        String normalize(String input);
    }

    public interface EndDateComputer {
        @NonNull
        String compute(@NonNull String start, int months);
    }

    public static final class FormData {
        public final String contractNumber;
        public final String fullName;
        public final String phoneNumber;
        public final String personalId;
        public final int memberCount;
        public final String signingDate;
        public final int contractMonths;
        public final int electricStartReading;
        public final int waterStartReading;
        public final boolean hasParkingService;
        public final int vehicleCount;
        public final boolean hasInternetService;
        public final boolean hasLaundryService;
        public final boolean remindOneMonthBefore;
        public final boolean showDepositOnInvoice;
        public final boolean showNoteOnInvoice;
        public final String billingReminderAt;
        public final double roomPrice;
        public final double depositAmount;
        public final String note;

        FormData(String contractNumber,
                String fullName,
                String phoneNumber,
                String personalId,
                int memberCount,
                String signingDate,
                int contractMonths,
                int electricStartReading,
                int waterStartReading,
                boolean hasParkingService,
                int vehicleCount,
                boolean hasInternetService,
                boolean hasLaundryService,
                boolean remindOneMonthBefore,
                boolean showDepositOnInvoice,
                boolean showNoteOnInvoice,
                String billingReminderAt,
                double roomPrice,
                double depositAmount,
                String note) {
            this.contractNumber = contractNumber;
            this.fullName = fullName;
            this.phoneNumber = phoneNumber;
            this.personalId = personalId;
            this.memberCount = memberCount;
            this.signingDate = signingDate;
            this.contractMonths = contractMonths;
            this.electricStartReading = electricStartReading;
            this.waterStartReading = waterStartReading;
            this.hasParkingService = hasParkingService;
            this.vehicleCount = vehicleCount;
            this.hasInternetService = hasInternetService;
            this.hasLaundryService = hasLaundryService;
            this.remindOneMonthBefore = remindOneMonthBefore;
            this.showDepositOnInvoice = showDepositOnInvoice;
            this.showNoteOnInvoice = showNoteOnInvoice;
            this.billingReminderAt = billingReminderAt;
            this.roomPrice = roomPrice;
            this.depositAmount = depositAmount;
            this.note = note;
        }
    }

    public static final class ValidationException extends Exception {
        ValidationException(@NonNull String message) {
            super(message);
        }
    }

    private ContractFormDataHelper() {
    }

    @NonNull
    public static FormData parseAndValidate(@NonNull String contractNumber,
            @NonNull String fullName,
            @NonNull String phoneNumber,
            @NonNull String personalId,
            @NonNull String memberCountInput,
            @NonNull String signingDateInput,
            @NonNull String contractMonthsInput,
            @NonNull String electricStartReadingInput,
            @NonNull String waterStartReadingInput,
            boolean electricMeterMode,
            boolean waterMeterMode,
            boolean hasParkingService,
            @NonNull String vehicleCountInput,
            boolean hasInternetService,
            boolean hasLaundryService,
            boolean remindOneMonthBefore,
            boolean showDepositOnInvoice,
            boolean showNoteOnInvoice,
            @NonNull String billingReminderAt,
            double roomPrice,
            double depositAmount,
            @NonNull String note,
            @NonNull String requiredFieldsMessage,
            @NonNull String invalidSigningDateMessage,
            @NonNull String vehicleCountRequiredMessage,
            @NonNull String invalidDataMessage,
            @NonNull MonthYearNormalizer monthYearNormalizer) throws ValidationException {
        if (fullName.isEmpty() || phoneNumber.isEmpty() || personalId.isEmpty() || memberCountInput.isEmpty()
                || signingDateInput.isEmpty()
                || contractMonthsInput.isEmpty()) {
            throw new ValidationException(requiredFieldsMessage);
        }

        if (electricMeterMode && electricStartReadingInput.isEmpty()) {
            throw new ValidationException(requiredFieldsMessage);
        }
        if (waterMeterMode && waterStartReadingInput.isEmpty()) {
            throw new ValidationException(requiredFieldsMessage);
        }

        String signingDate = monthYearNormalizer.normalize(signingDateInput);
        if (signingDate.isEmpty()) {
            throw new ValidationException(invalidSigningDateMessage);
        }

        if (hasParkingService && vehicleCountInput.isEmpty()) {
            throw new ValidationException(vehicleCountRequiredMessage);
        }

        int memberCount;
        int contractMonths;
        int electricStartReading = 0;
        int waterStartReading = 0;
        int vehicleCount = 0;
        try {
            memberCount = Integer.parseInt(memberCountInput);
            contractMonths = Integer.parseInt(contractMonthsInput);
            if (electricMeterMode) {
                electricStartReading = Integer.parseInt(electricStartReadingInput);
            }
            if (waterMeterMode) {
                waterStartReading = Integer.parseInt(waterStartReadingInput);
            }
            if (hasParkingService) {
                vehicleCount = Integer.parseInt(vehicleCountInput);
            }
        } catch (Exception e) {
            throw new ValidationException(invalidDataMessage);
        }

        if (hasParkingService && vehicleCount <= 0) {
            throw new ValidationException(vehicleCountRequiredMessage);
        }

        return new FormData(
                contractNumber,
                fullName,
                phoneNumber,
                personalId,
                memberCount,
                signingDate,
                contractMonths,
                electricStartReading,
                waterStartReading,
                hasParkingService,
                vehicleCount,
                hasInternetService,
                hasLaundryService,
                remindOneMonthBefore,
                showDepositOnInvoice,
                showNoteOnInvoice,
                billingReminderAt,
                roomPrice,
                depositAmount,
                note);
    }

    public static void applyToContract(@NonNull Tenant contract,
            @NonNull FormData data,
            @NonNull String roomId,
            String roomNumber,
            @NonNull EndDateComputer endDateComputer) {
        contract.setContractNumber(data.contractNumber);
        contract.setFullName(data.fullName);
        contract.setPhoneNumber(data.phoneNumber);
        contract.setAddress("");
        contract.setPersonalId(data.personalId);
        contract.setRoomId(roomId);
        contract.setRoomNumber(roomNumber);
        contract.setMemberCount(data.memberCount);
        contract.setRentalStartDate(data.signingDate);
        contract.setContractDurationMonths(data.contractMonths);
        contract.setRemindOneMonthBefore(data.remindOneMonthBefore);
        String billingStartPolicy = normalizeBillingStartPolicy(data.billingReminderAt);
        contract.setBillingStartPolicy(billingStartPolicy);
        contract.setBillingStartPeriod(computeBillingStartPeriod(data.signingDate, billingStartPolicy));
        contract.setBillingReminderAt(toLegacyBillingReminder(billingStartPolicy));

        // Persist canonical monetary fields.
        contract.setRentAmount((long) data.roomPrice);
        contract.setDepositAmount((long) data.depositAmount);
        contract.setShowDepositOnInvoice(data.showDepositOnInvoice);
        contract.setElectricStartReading(data.electricStartReading);
        contract.setWaterStartReading(data.waterStartReading);
        contract.setHasParkingService(data.hasParkingService);
        contract.setVehicleCount(data.vehicleCount);
        contract.setHasInternetService(data.hasInternetService);
        contract.setHasLaundryService(data.hasLaundryService);
        contract.setNote(data.note);
        contract.setShowNoteOnInvoice(data.showNoteOnInvoice);
        contract.setContractStatus("ACTIVE");
        contract.setContractEndDate(endDateComputer.compute(data.signingDate, data.contractMonths));
    }

    @NonNull
    private static String normalizeBillingStartPolicy(String value) {
        if (value == null) {
            return "current_month";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("next_month".equals(normalized) || "mid_month".equals(normalized) || "end_month".equals(normalized)) {
            return "next_month";
        }
        return "current_month";
    }

    @NonNull
    private static String computeBillingStartPeriod(String signingDate, String billingStartPolicy) {
        Calendar calendar = ContractDateHelper.parseContractDate(signingDate);
        if (calendar == null) {
            return "";
        }
        if ("next_month".equals(billingStartPolicy)) {
            calendar.add(Calendar.MONTH, 1);
        }
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        return String.format(Locale.US, "%02d/%04d", month, year);
    }

    @NonNull
    private static String toLegacyBillingReminder(String billingStartPolicy) {
        return "next_month".equals(billingStartPolicy) ? "mid_month" : "start_month";
    }
}
