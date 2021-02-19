package gm.tieba.tabswitch.ui;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Map;

import gm.tieba.tabswitch.databinding.AdapterItemModuleListBinding;
import gm.tieba.tabswitch.util.RepackageProcessor;

public class ModuleListAdapter extends RecyclerView.Adapter<ModuleListAdapter.ViewHolder> {
    private final Activity activity;
    private OnItemClickListener onItemClickListener;

    public ModuleListAdapter(MainActivity mainActivity) {
        activity = mainActivity;
    }

    @NonNull
    @Override
    public ModuleListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AdapterItemModuleListBinding item = AdapterItemModuleListBinding.inflate(LayoutInflater.from(parent.getContext()));
        return new ModuleListAdapter.ViewHolder(item);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        AdapterItemModuleListBinding binding;

        public ViewHolder(@NonNull AdapterItemModuleListBinding item) {
            super(item.getRoot());
            binding = item;
            binding.getRoot().setOnClickListener(v -> {
                if (onItemClickListener != null)
                    onItemClickListener.OnItemClick(item.getRoot(), getLayoutPosition());
            });
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> map = RepackageProcessor.moduleList.get(position);
        holder.binding.cb.setChecked((Boolean) map.get("isChecked"));
        PackageManager pm = activity.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo((String) map.get("packageName"), PackageManager.GET_ACTIVITIES);
            holder.binding.iv.setImageDrawable(pi.applicationInfo.loadIcon(pm));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        holder.binding.tvApkName.setText((String) map.get("apkName"));
        holder.binding.tvPackageName.setText((String) map.get("packageName"));
    }

    @Override
    public int getItemCount() {
        return RepackageProcessor.moduleList.size();
    }

    public interface OnItemClickListener {
        void OnItemClick(View view, int layoutPosition);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}
