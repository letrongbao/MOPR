package com.example.myapplication.features.contract;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.WaterCalculationMode;
import com.example.myapplication.domain.House;
import com.example.myapplication.domain.Tenant;
import com.example.myapplication.domain.Room;

import java.text.NumberFormat;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

public final class ContractHtmlBuilder {

    private ContractHtmlBuilder() {
    }

    @NonNull
    public static String buildContractHtml(@NonNull Context context, @NonNull Tenant contract, Room room, House house) {
        String roomNumber = room != null ? nullToEmpty(room.getRoomNumber()) : "";
        String propertyAddress = house != null ? nullToEmpty(house.getAddress()) : "";
        String landlordName = house != null ? nullToEmpty(house.getHouseName()) : "";
        String landlordPhone = house != null ? nullToEmpty(house.getManagerPhone()) : "";
        String rentalStartDate = nullToEmpty(contract.getRentalStartDate());
        String contractEndTimestamp = nullToEmpty(contract.getContractEndDate());
        int contractMonths = contract.getContractDurationMonths();

        StringBuilder expense = new StringBuilder();
        if (house != null) {
            expense.append(context.getString(
                    R.string.contract_expense_electricity_line,
                    formatVnd(house.getElectricityPrice()),
                    contract.getElectricStartReading()));

            String waterCalculationMethod = house.getWaterCalculationMethod();
            String waterUnit = WaterCalculationMode.isPerPerson(waterCalculationMethod)
                    ? context.getString(R.string.contract_water_unit_per_person)
                    : WaterCalculationMode.isMeter(waterCalculationMethod)
                            ? context.getString(R.string.contract_water_unit_meter)
                            : context.getString(R.string.contract_water_unit_room);
            expense.append(context.getString(R.string.contract_expense_water_line, formatVnd(house.getWaterPrice()),
                    waterUnit));

            if (contract.hasParkingService()) {
                expense.append(context.getString(
                        R.string.contract_expense_parking_line,
                        formatVnd(house.getParkingPrice()),
                        getUnitLabel(context, house.getParkingUnit(), context.getString(R.string.contract_unit_vehicle)),
                        contract.getVehicleCount()));
            }
            if (contract.hasInternetService()) {
                expense.append(context.getString(R.string.contract_expense_internet_line,
                        formatVnd(house.getInternetPrice()),
                        getUnitLabel(context, house.getInternetUnit(), context.getString(R.string.contract_unit_room))));
            }

                if (contract.hasLaundryService() && house.getLaundryPrice() > 0) {
                expense.append(
                        context.getString(
                                R.string.contract_expense_laundry_line,
                                formatVnd(house.getLaundryPrice()),
                                getUnitLabel(context, house.getLaundryUnit(),
                                        context.getString(R.string.contract_unit_room))));
            }
            if (house.getTrashPrice() > 0) {
                expense.append(
                        context.getString(
                                R.string.contract_expense_trash_line,
                                formatVnd(house.getTrashPrice()),
                                getUnitLabel(context, house.getTrashUnit(),
                                        context.getString(R.string.contract_unit_room))));
            }

            appendExtraFees(context, house, contract, expense);
        }

        String template = readRawText(context, R.raw.contract_template_vi);
        return template
                .replace("{{DATE_FULL}}", escape(formatDateFull(rentalStartDate)))
                .replace("{{PROPERTY_ADDRESS}}", escape(propertyAddress))
                .replace("{{LANDLORD_NAME}}", escape(landlordName))
                .replace("{{LANDLORD_PHONE}}", escape(landlordPhone))
                .replace("{{TENANT_FULL_NAME}}", escape(contract.getFullName()))
                .replace("{{TENANT_PERSONAL_ID}}", escape(contract.getPersonalId()))
                .replace("{{TENANT_PHONE}}", escape(contract.getPhoneNumber()))
                .replace("{{ROOM_NUMBER}}", escape(roomNumber))
                .replace("{{CONTRACT_MONTHS}}", String.valueOf(contractMonths))
                .replace("{{RENTAL_START_DATE}}", escape(rentalStartDate))
                .replace("{{CONTRACT_END_DATE}}", escape(contractEndTimestamp))
                .replace("{{RENT_AMOUNT}}", formatVnd(contract.getRentAmount()))
                .replace("{{EXPENSE_LINES}}", expense.toString())
                .replace("{{DEPOSIT_AMOUNT}}", formatVnd(contract.getDepositAmount()))
                .replace("{{MEMBER_COUNT}}", String.valueOf(contract.getMemberCount()));
    }

    @NonNull
    private static String formatDateFull(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return ".../.../...";
        }
        try {
            String[] parts = dateStr.split("/");
            if (parts.length == 3) {
                return parts[0] + "/" + parts[1] + "/" + parts[2];
            }
        } catch (Exception ignored) {
        }
        return dateStr;
    }

    @NonNull
    private static String readRawText(@NonNull Context context, int rawResId) {
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = context.getResources().openRawResource(rawResId);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String content = sb.toString();
            return TextUtils.isEmpty(content) ? "" : content;
        } catch (Exception ignored) {
            return "";
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    @NonNull
    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @NonNull
    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    @NonNull
    private static String formatVnd(double value) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format((long) value) + " ₫";
    }

    @NonNull
    private static String getUnitLabel(@NonNull Context context, String unitValue, @NonNull String defaultLabel) {
        if (unitValue == null || unitValue.trim().isEmpty()) {
            return defaultLabel;
        }
        String normalized = unitValue.trim().toLowerCase(Locale.ROOT);
        if ("person".equals(normalized)) {
            return context.getString(R.string.contract_unit_person);
        }
        if ("vehicle".equals(normalized)) {
            return context.getString(R.string.contract_unit_vehicle);
        }
        if ("room".equals(normalized)) {
            return context.getString(R.string.contract_unit_room);
        }
        return unitValue;
    }

    private static void appendExtraFees(
            @NonNull Context context,
            @NonNull House house,
            @NonNull Tenant contract,
            @NonNull StringBuilder expense) {
        java.util.List<House.ExtraFee> extraFees = house.getExtraFees();
        if (extraFees == null || extraFees.isEmpty()) {
            return;
        }

        List<String> selectedExtraFeeNames = contract.getSelectedExtraFeeNames();
        boolean hasSelectedFilters = selectedExtraFeeNames != null && !selectedExtraFeeNames.isEmpty();

        for (House.ExtraFee fee : extraFees) {
            if (fee == null)
                continue;
            String name = fee.getFeeName() != null ? fee.getFeeName().trim() : "";
            if (name.isEmpty() || fee.getPrice() <= 0)
                continue;

            if (hasSelectedFilters && !containsNormalizedKey(selectedExtraFeeNames, name)) {
                continue;
            }

            String unit = fee.getUnit() != null ? fee.getUnit().trim() : "";
            String unitSuffix = unit.isEmpty() ? "" : "/" + unit;
            expense.append(context.getString(
                    R.string.contract_expense_extra_line,
                    name,
                    formatVnd(fee.getPrice()),
                    unitSuffix));
        }
    }

    private static boolean containsNormalizedKey(@NonNull List<String> keys, String target) {
        String normalizedTarget = normalizeFeeKey(target);
        if (normalizedTarget.isEmpty()) {
            return false;
        }
        for (String key : keys) {
            if (normalizedTarget.equals(normalizeFeeKey(key))) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static String normalizeFeeKey(String feeName) {
        if (feeName == null) {
            return "";
        }
        return feeName.trim().toLowerCase(Locale.ROOT);
    }
}
