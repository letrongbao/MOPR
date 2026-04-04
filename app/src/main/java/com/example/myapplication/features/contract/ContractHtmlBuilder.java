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
                        contract.getVehicleCount()));
            }
            if (contract.hasInternetService()) {
                expense.append(context.getString(R.string.contract_expense_internet_line,
                        formatVnd(house.getInternetPrice())));
            }
            if (contract.hasLaundryService()) {
                expense.append(
                        context.getString(R.string.contract_expense_laundry_line, formatVnd(house.getLaundryPrice())));
            }
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
}
