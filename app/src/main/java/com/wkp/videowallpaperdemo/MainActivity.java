package com.wkp.videowallpaperdemo;

import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.wkp.runtimepermissions.callback.PermissionCallBack;
import com.wkp.runtimepermissions.util.RuntimePermissionUtil;
import com.wkp.videowallpaper.bean.VideoInfo;
import com.wkp.videowallpaper.util.VideoUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private List<VideoInfo> mVideoInfos = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取列表控件
        mRecyclerView = findViewById(R.id.rv);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        mRecyclerView.setAdapter(adapter);
        //申请运行时权限
        RuntimePermissionUtil.checkPermissions(this, RuntimePermissionUtil.STORAGE, new PermissionCallBack() {
            @Override
            public void onCheckPermissionResult(boolean hasPermission) {
                if (hasPermission) {
                    //获取外部存储视频文件
                    VideoUtils.getVideoFile(mVideoInfos, Environment.getExternalStorageDirectory());
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    /**
     * 设置视频桌面
     * @param file
     * @param isVolume
     */
    private void sendReceive(final File file, final boolean isVolume) {
        VideoUtils.setVideoWallpaper(this,file,isVolume);
    }

    /**
     * 适配器
     */
    RecyclerView.Adapter adapter = new RecyclerView.Adapter<MyViewHolder>() {
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            MyViewHolder holder = new MyViewHolder(View.inflate(parent.getContext(), R.layout.item_rv, null));
            return holder;
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            final VideoInfo videoInfo = mVideoInfos.get(position);
            Glide.with(MainActivity.this)
                    .load(Uri.fromFile(videoInfo.file))
                    .apply(new RequestOptions().placeholder(R.mipmap.ic_launcher))
                    .into(holder.mIv);
            holder.mTv.setText(videoInfo.displayName);
            holder.mTv.setTextColor(position % 2 == 0 ? Color.BLUE : Color.GRAY);
            //点击缩略图弹出对话框，设置有/无声音的视频桌面
            holder.mIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    new MaterialDialog.Builder(v.getContext())
                            .title("声音开启")
                            .titleGravity(GravityEnum.CENTER)
                            .content("是否开启声音？")
                            .negativeText("否")
                            .positiveText("是")
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    sendReceive(videoInfo.file,false);
                                }
                            }).onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            sendReceive(videoInfo.file,true);
                        }
                    }).show();
                }
            });
            //长按缩略图弹出对话框，是否删除文件
            holder.mIv.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(final View v) {
                    new MaterialDialog.Builder(v.getContext())
                            .title("删除文件")
                            .titleGravity(GravityEnum.CENTER)
                            .content("是否删除该文件？")
                            .negativeText("否")
                            .positiveText("是")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            boolean delete = videoInfo.file.delete();
                            if (delete) {
                                mVideoInfos.remove(videoInfo);
                                adapter.notifyDataSetChanged();
                                Toast.makeText(v.getContext(), "文件已删除", Toast.LENGTH_SHORT).show();
                            }else {
                                Toast.makeText(v.getContext(), "文件删除失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).show();
                    return false;
                }
            });
            //点击文件名弹出对话框，修改文件名
            holder.mTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final EditText text = new EditText(v.getContext());
                    new MaterialDialog.Builder(v.getContext())
                            .title("修改文件名")
                            .titleGravity(GravityEnum.CENTER)
                            .customView(text, false)
                            .negativeText("取消")
                            .positiveText("确定")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    String name = text.getText().toString().trim();
                                    if (!TextUtils.isEmpty(name)) {
                                        String fuxName = videoInfo.displayName.substring(videoInfo.displayName.indexOf("."));
                                        String displayName = name + fuxName;
                                        String filePath = videoInfo.filePath.substring(0, videoInfo.filePath.lastIndexOf("/") + 1) + videoInfo.displayName;
                                        File dest = new File(filePath);
                                        boolean renameTo = videoInfo.file.renameTo(dest);
                                        if (renameTo) {
                                            videoInfo.displayName = displayName;
                                            videoInfo.filePath = filePath;
                                            adapter.notifyDataSetChanged();
                                        }else {
                                            Toast.makeText(v.getContext(), "修改失败", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(v.getContext(), "文件名不能为空", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mVideoInfos.size();
        }
    };

    /**
     * 适配器帮助类
     */
    class MyViewHolder extends RecyclerView.ViewHolder {

        private ImageView mIv;
        private TextView mTv;

        public MyViewHolder(View itemView) {
            super(itemView);
            mIv = itemView.findViewById(R.id.iv_item);
            mTv = itemView.findViewById(R.id.tv_item);
        }
    }

}
