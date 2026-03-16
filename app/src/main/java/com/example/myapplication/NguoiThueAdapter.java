package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.model.NguoiThue;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NguoiThueAdapter extends RecyclerView.Adapter<NguoiThueAdapter.ViewHolder> {

    private List<NguoiThue> danhSach = new ArrayList<>();
    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onXoa(NguoiThue nguoiThue);
        void onSua(NguoiThue nguoiThue);
    }

    public NguoiThueAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setDanhSach(List<NguoiThue> list) {
        this.danhSach = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nguoi_thue, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NguoiThue n = danhSach.get(position);
        holder.tvHoTen.setText(n.getHoTen());
        String tenPhong = n.getSoPhong() != null ? "Phòng " + n.getSoPhong() : "Phòng: " + n.getIdPhong();
        holder.tvPhong.setText(tenPhong);
        holder.tvCccd.setText("CCCD: " + n.getCccd());
        holder.tvSdt.setText("SĐT: " + n.getSoDienThoai());
        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        holder.tvTienCoc.setText("Cọc: " + fmt.format(n.getTienCoc()) + " đ");
        holder.btnXoa.setOnClickListener(v -> listener.onXoa(n));
        holder.btnSua.setOnClickListener(v -> listener.onSua(n));
    }

    @Override
    public int getItemCount() {
        return danhSach.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHoTen, tvPhong, tvCccd, tvSdt, tvTienCoc;
        ImageButton btnXoa, btnSua;

        ViewHolder(View v) {
            super(v);
            tvHoTen = v.findViewById(R.id.tvHoTen);
            tvPhong = v.findViewById(R.id.tvPhong);
            tvCccd = v.findViewById(R.id.tvCccd);
            tvSdt = v.findViewById(R.id.tvSdt);
            tvTienCoc = v.findViewById(R.id.tvTienCoc);
            btnXoa = v.findViewById(R.id.btnXoa);
            btnSua = v.findViewById(R.id.btnSua);
        }
    }
}
