package com.example.myapplication.features.contract;

import androidx.annotation.NonNull;

import com.example.myapplication.domain.House;
import com.example.myapplication.domain.Tenant;
import com.example.myapplication.domain.Room;

import java.text.NumberFormat;
import java.util.Locale;

public final class ContractHtmlBuilder {

    private ContractHtmlBuilder() {
    }

    @NonNull
    public static String buildContractHtml(@NonNull Tenant contract, Room room, House house) {
        String soPhong = room != null ? nullToEmpty(room.getSoPhong()) : "";
        String khuDiaChi = house != null ? nullToEmpty(house.getDiaChi()) : "";
        String tenChuTro = house != null ? nullToEmpty(house.getTenHouse()) : "";
        String sdtChuTro = house != null ? nullToEmpty(house.getSdtQuanLy()) : "";
        String ngayKy = nullToEmpty(contract.getNgayBatDauThue());
        String ngayKetThuc = nullToEmpty(contract.getNgayKetThucHopDong());
        int soThang = contract.getSoThangHopDong();

        StringBuilder chiPhi = new StringBuilder();
        if (house != null) {
            chiPhi.append("- Tiền điện: ").append(formatVnd(house.getGiaDien()))
                    .append("/kWh (Chỉ số ban đầu: ").append(contract.getChiSoDienDau()).append(")<br/>");

            String cachTinhNuoc = house.getCachTinhNuoc();
            String donViNuoc = "nguoi".equals(cachTinhNuoc)
                    ? "/người/tháng"
                    : "dong_ho".equals(cachTinhNuoc) ? "/m³" : "/phòng";
            chiPhi.append("- Tiền nước: ").append(formatVnd(house.getGiaNuoc())).append(donViNuoc).append("<br/>");

            if (contract.isDichVuGuiXe()) {
                chiPhi.append("- Phí gửi xe: ").append(formatVnd(house.getGiaXe()))
                        .append("/chiếc (SL: ").append(contract.getSoLuongXe()).append(")<br/>");
            }
            if (contract.isDichVuInternet()) {
                chiPhi.append("- Internet: ").append(formatVnd(house.getGiaInternet())).append("/tháng<br/>");
            }
            if (contract.isDichVuGiatSay()) {
                chiPhi.append("- Giặt sấy: ").append(formatVnd(house.getGiaGiatSay())).append("/tháng<br/>");
            }
        }

        return "<!DOCTYPE html><html><head><meta charset='utf-8'/><style>body{font-family:'Times New Roman',serif;font-size:13px;padding:30px 40px;line-height:1.6;color:#000;}.header{text-align:center;margin-bottom:20px;}.header h3{margin:0;font-size:14px;font-weight:bold;text-transform:uppercase;}.header p{margin:2px 0;font-size:12px;font-style:italic;}.title{text-align:center;margin:25px 0;}.title h2{margin:0;font-size:18px;font-weight:bold;text-transform:uppercase;}.info{margin-bottom:8px;}.section{margin-top:15px;}.section-title{font-weight:bold;margin-bottom:5px;}.indent{padding-left:20px;}.gia-han-table{width:100%;border-collapse:collapse;margin:10px 0;}.gia-han-table th,.gia-han-table td{border:1px solid #000;padding:6px 8px;text-align:center;font-size:12px;}.gia-han-table th{background:#f5f5f5;font-weight:bold;}.signature{display:flex;justify-content:space-around;margin-top:40px;text-align:center;}.signature div{width:40%;}.signature .label{font-weight:bold;text-transform:uppercase;}.signature .note{font-style:italic;font-size:11px;}.signature .name{margin-top:60px;font-weight:bold;}</style></head><body><div class='header'><h3>CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</h3><p>Độc lập – Tự do – Hạnh phúc</p></div><div class='title'><h2>HỢP ĐỒNG THUÊ PHÒNG TRỌ</h2></div><p class='info'>Hôm nay, ngày "
                + formatDateFull(ngayKy) + ", tại căn nhà số " + escape(khuDiaChi)
                + "</p><p class='info'>Chúng tôi ký tên dưới đây gồm có:</p><div class='section'><p class='section-title'>BÊN CHO THUÊ PHÒNG TRỌ (gọi tắt là Bên A):</p><p class='indent'>Ông/bà: "
                + escape(tenChuTro)
                + "</p><p class='indent'>CMND/CCCD số: .................. cấp ngày: ............... nơi cấp: ...............</p><p class='indent'>Điện thoại: "
                + escape(sdtChuTro) + "</p><p class='indent'>Thường trú tại: " + escape(khuDiaChi)
                + "</p></div><div class='section'><p class='section-title'>BÊN THUÊ PHÒNG TRỌ (gọi tắt là Bên B):</p><p class='indent'>Ông/bà: "
                + escape(contract.getHoTen()) + "</p><p class='indent'>CMND/CCCD số: " + escape(contract.getCccd())
                + " cấp ngày: ............... nơi cấp: ...............</p><p class='indent'>Điện thoại: "
                + escape(contract.getSoDienThoai())
                + "</p></div><p style='margin-top:15px;'>Sau khi thỏa thuận, hai bên thống nhất như sau:</p><div class='section'><p class='section-title'>1. Nội dung thuê phòng trọ</p><p class='indent'>Bên A cho Bên B thuê 01 phòng trọ số "
                + escape(soPhong) + " tại căn nhà số " + escape(khuDiaChi)
                + ".</p><p class='indent'>Với thời hạn là: "
                + soThang + " tháng (Từ ngày " + escape(ngayKy) + " đến ngày " + escape(ngayKetThuc)
                + ").</p><p class='indent'>Giá thuê: " + formatVnd(contract.getTienPhong())
                + "/tháng.</p><p class='indent'>Chưa bao gồm chi phí:</p><p class='indent'>" + chiPhi
                + "</p></div><div class='section'><p class='section-title'>2. Trách nhiệm Bên A</p><p class='indent'>- Đảm bảo căn nhà cho thuê không có tranh chấp, khiếu kiện.</p><p class='indent'>- Đăng ký với chính quyền địa phương về thủ tục cho thuê phòng trọ.</p></div><div class='section'><p class='section-title'>3. Trách nhiệm Bên B</p><p class='indent'>- Đặt cọc với số tiền là "
                + formatVnd(contract.getTienCoc())
                + ", thanh toán tiền thuê phòng hàng tháng + tiền điện + nước.</p><p class='indent'>- Đảm bảo các thiết bị và sửa chữa các hư hỏng trong phòng trong khi sử dụng. Nếu không sửa chữa thì khi trả phòng, bên A sẽ trừ vào tiền đặt cọc, giá trị cụ thể tính theo giá thị trường.</p><p class='indent'>- Chỉ sử dụng phòng trọ vào mục đích ở, với số lượng tối đa không quá "
                + contract.getSoThanhVien()
                + " người; không chứa các thiết bị gây cháy nổ, hàng cấm; cung cấp giấy tờ tùy thân để đăng ký tạm trú theo quy định, giữ gìn an ninh trật tự, nếp sống văn hóa đô thị; không tụ tập nhậu nhẹt, cờ bạc và các hành vi vi phạm pháp luật khác.</p><p class='indent'>- Không được tự ý cải tạo kiến trúc phòng hoặc trang trí ảnh hưởng tới tường, cột, nền... Nếu có nhu cầu trên phải trao đổi với bên A để được thống nhất.</p></div><div class='section'><p class='section-title'>4. Điều khoản thực hiện</p><p class='indent'>Hai bên nghiêm túc thực hiện những quy định trên trong thời hạn cho thuê, nếu bên A lấy phòng phải báo cho bên B ít nhất 01 tháng, hoặc ngược lại.</p><p class='indent'>Sau thời hạn cho thuê "
                + soThang
                + " tháng nếu bên B có nhu cầu hai bên tiếp tục thương lượng giá thuê để gia hạn hợp đồng.</p></div><div class='section'><p class='section-title'>Bảng gia hạn hợp đồng:</p><table class='gia-han-table'><tr><th>Lần</th><th>Thời gian<br/>(tháng)</th><th>Từ ngày</th><th>Đến ngày</th><th>Giá thuê/tháng</th><th>Ký tên</th></tr><tr><td>1</td><td></td><td></td><td></td><td></td><td></td></tr><tr><td>2</td><td></td><td></td><td></td><td></td><td></td></tr></table></div><div class='signature'><div><p class='label'>BÊN B</p><p class='note'>(Ký, ghi rõ họ tên)</p><p class='name'>"
                + escape(contract.getHoTen())
                + "</p></div><div><p class='label'>BÊN A</p><p class='note'>(Ký, ghi rõ họ tên)</p><p class='name'>"
                + escape(tenChuTro) + "</p></div></div></body></html>";
    }

    @NonNull
    private static String formatDateFull(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "... tháng ... năm ...";
        }
        try {
            String[] parts = dateStr.split("/");
            if (parts.length == 3) {
                return parts[0] + " tháng " + parts[1] + " năm " + parts[2];
            }
        } catch (Exception ignored) {
        }
        return dateStr;
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
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format((long) value) + " đ";
    }
}
