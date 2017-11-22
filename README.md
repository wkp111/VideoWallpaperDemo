# VideoWallpaperDemo [ ![Download](https://api.bintray.com/packages/wkp/maven/VideoWallpaper/images/download.svg) ](https://bintray.com/wkp/maven/VideoWallpaper/_latestVersion)
快速设置动态视频桌面
## 演示图
![DragGridView](https://github.com/wkp111/VideoWallpaperDemo/blob/master/VideoWallpaper.gif "演示图")
## Gradle集成
```groovy
dependencies{
      compile 'com.wkp:VideoWallpaper:1.0.3'
      //Android Studio3.0+可用以下方式
      //implementation 'com.wkp:VideoWallpaper:1.0.3'
}
```
Note：可能存在Jcenter还在审核阶段，这时会集成失败！
## 使用详解
> 可变参数示例
```xml
<!--video_wallpaper.xml-->
<?xml version="1.0" encoding="utf-8"?>
<wallpaper xmlns:android="http://schemas.android.com/apk/res/android"
           android:author="@string/author"
           android:description="@string/description"
           android:thumbnail="@mipmap/ic_launcher"> <!--缩略图-->
</wallpaper>
```
Note：其中属性值都可以自己设置，自己重写XML文件亦可。
> 布局示例
```xml
<!--activity_main.xml-->
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <android.support.v7.widget.RecyclerView
        android:id="@+id/rv"
        android:layout_marginTop="5dp"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

    </android.support.v7.widget.RecyclerView>

</LinearLayout>

<!--item_rv.xml-->
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
              android:orientation="vertical"
              android:gravity="center_horizontal"
              android:layout_width="match_parent"
              android:layout_height="wrap_content">

    <ImageView
        android:id="@+id/iv_item"
        android:scaleType="fitXY"
        android:layout_width="150dp"
        android:layout_height="150dp"/>

    <TextView
        android:id="@+id/tv_item"
        android:gravity="center_horizontal"
        android:layout_marginTop="10dp"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"/>

</LinearLayout>
```
Note：该布局示例仅仅是配合库的使用！
> 代码示例
```java
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
        //添加设置视频桌面响应监听（回调有重复调用的情况）
        VideoUtils.addOnVideoWallpaperRespListener(onVideoWallpaperRespListener);
    }

    //设置视频桌面响应监听
    private VideoRespReceive.OnVideoWallpaperRespListener onVideoWallpaperRespListener = new VideoRespReceive.OnVideoWallpaperRespListener() {
        @Override
        public void onVideoWallpaperResp(@Nullable File file, int respState) {
            //file对应设置的视频文件，可能为空； respState为响应码，对应如下：
            // public static final int RESP_SUCCESS = 100;          桌面设置成功
            // public static final int RESP_FAILED_NULL = 101;      视频文件为空
            // public static final int RESP_FAILED_ERROR = 102;     视频播放异常
            // public static final int RESP_FAILED_INIT = 103;      引擎无播放控件
            if (file != null) {
                Log.d("MainActivity", file.getName());
            }
            Log.d("MainActivity", "respState:" + respState);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //注意：为了防止内存泄露，一定要记得移除监听
        VideoUtils.removeOnVideoWallpaperRespListener(onVideoWallpaperRespListener);
//        VideoUtils.removeAllOnVideoWallpaperRespListener();
    }

    /**
     * 设置视频桌面
     * @param file
     * @param isVolume
     */
    private void sendReceive(final File file, final boolean isVolume) {
        //控制延迟时间（当壁纸界面为黑屏时可以适当将其设置长一点，看手机好坏了，一般一秒(1000)应该够了）
        VideoUtils.sSendTime = 800;
        //设置视频桌面（默认Toast为false）
//        VideoUtils.setVideoWallpaper(this,file,isVolume);
        VideoUtils.setVideoWallpaper(this,file,isVolume,true);
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
                                        String filePath = videoInfo.filePath.substring(0, videoInfo.filePath.lastIndexOf("/") + 1) + displayName;
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
```
Note：大部分API都经过<a href="https://github.com/wkp111/VideoWallpaperDemo/blob/master/videowallpaper/src/main/java/com/wkp/videowallpaper/util/VideoUtils.java">VideoUtils.java</a>进行了封装，开发者亦可自己阅读源码进行扩展构造，内部通信实质为BroadcastReceive。
## 感谢
matrial风格对话框：<a href="https://github.com/afollestad/material-dialogs">afollestad/material-dialogs</a>（Github地址）<br/>
Android6.0动态权限：<a href="https://github.com/wkp111/RuntimePermission">wkp111/RuntimePermission</a>（Github地址）<br/>
花式多样图片加载：<a href="https://github.com/bumptech/glide">bumptech/glide</a>（Github地址）
## 寄语
控件支持直接代码创建，还有更多API请观看<a href="https://github.com/wkp111/VideoWallpaperDemo/blob/master/videowallpaper/src/main/java/com/wkp/videowallpaper/util/VideoUtils.java">VideoUtils.java</a>内的注释说明。<br/>
欢迎大家使用，感觉好用请给个Star鼓励一下，谢谢！<br/>
大家如果有更好的意见或建议以及好的灵感，请邮箱作者，谢谢！<br/>
QQ邮箱：1535514884@qq.com<br/>
163邮箱：15889686524@163.com<br/>
Gmail邮箱：wkp15889686524@gmail.com<br/>

## 版本更新
* v1.0.3
改善设置界面黑屏方式（设置延迟时长）
* v1.0.2
增加设置视频桌面响应监听
* v1.0.1
新创建设置视频桌面库
## License

   Copyright 2017 wkp

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
