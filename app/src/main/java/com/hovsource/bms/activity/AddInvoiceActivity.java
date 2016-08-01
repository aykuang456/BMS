package com.hovsource.bms.activity;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hovsource.bms.activity.bms.R;
import com.hovsource.bms.config.AppSession;
import com.hovsource.bms.config.ConstOptions;
import com.hovsource.bms.config.ServiceConst;
import com.hovsource.bms.core.ConstOption;
import com.hovsource.bms.core.DTBlock;
import com.hovsource.bms.core.DTInfo;
import com.hovsource.bms.core.DTRow;
import com.hovsource.bms.dao.ServiceMapping;
import com.hovsource.bms.service.FolderService;
import com.hovsource.bms.util.ImageLoader;
import com.hovsource.bms.util.ToolsUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AddInvoiceActivity extends BaseActivity {
    private static final int CAMERA_WITH_DATA = 3023;
    public RelativeLayout ibtn_photo;
    public RelativeLayout ibtn_upload;
    private List<String> mUrlStrs = new ArrayList<String>();       //图片集合
    private ImageLoader mImageLoader;                              //图片加载引擎
    private String imagefile;                                       //拍照图片路径
    private LinearLayout ll_delete;                                 //删除布局
    private LinearLayout ll_tools;                                  //工具布局
    private ListImgItemAdaper adaper;                               //数据适配器
    private GridView gv_images;                                     //图片预览
    private Boolean isEdit = false;                                 //是否为编辑模式
    private List<String> selectItem = new ArrayList<String>();      //选中的图片集合
    private ProgressDialog pd_schedule; //进度条信息

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_invoice);
        intView();
    }

    public void intView() {
        mImageLoader = ImageLoader.getInstance(3, ImageLoader.Type.LIFO);
        ServiceMapping mapp = new ServiceMapping();
        //设置拍照监听事件
        ibtn_photo = findViewById_(R.id.ib_photo);
        ibtn_photo.setOnClickListener(photoClickListener);
        //获取删除监听事件
        ll_delete = findViewById_(R.id.ll_delete);
        ll_delete.setOnClickListener(delClickListener);
        //获取工具栏布局
        ll_tools = findViewById_(R.id.ll_tools);
        //设置上传监听事件
        ibtn_upload = findViewById_(R.id.ib_upload);
        ibtn_upload.setOnClickListener(uploadOnClickListener);
        gv_images = findViewById_(R.id.gv_images);
        adaper = new ListImgItemAdaper(AddInvoiceActivity.this);
        gv_images.setAdapter(adaper);
        gv_images.setOnItemClickListener(itemClickListener);
        gv_images.setOnItemLongClickListener(itemLongClickListener);
    }


    private class ListImgItemAdaper extends BaseAdapter {
        private LayoutInflater mInflater;// 动态布局映射

        public ListImgItemAdaper(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mUrlStrs.size();
        }

        @Override
        public Object getItem(int i) {
            return mUrlStrs.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }
        /**
         * 添加选中状态的图片位置
         */
        public void addNumber(String item){
            selectItem.add(item);
        }
        /**
         * 去除已选中状态的图片位置
         */
        public void delNumber(String item){
            selectItem.remove(item);
        }
        /**
         * 清空已选中的图片状态
         */
        public void clear(){
            selectItem.clear();
            notifyDataSetChanged();
        }
        /**
         * 添加图片
         */
        public void addPhoto(String imgPath){
            mUrlStrs.add(imgPath);
            notifyDataSetChanged();
        }
        /**
         * 删除图片
         * @param loadimgLs
         */
        public void deletePhoto(List<String> loadimgLs){
            for(String img:loadimgLs){
                if(mUrlStrs.contains(img)){
                    mUrlStrs.remove(img);
                }
            }
            selectItem.clear();
            notifyDataSetChanged();
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final int pos = position;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(
                        R.layout.layout_grid_item, parent, false);
            }
            ImageView imageview = (ImageView) convertView.findViewById(R.id.id_img);
            RelativeLayout layout = (RelativeLayout) convertView.findViewById(R.id.grid_item);
            ImageView stateView = (ImageView) convertView.findViewById(R.id.scan_select);
            imageview.setImageResource(R.drawable.png_pictures_no);
            String imgItem = getItem(position).toString();
            mImageLoader.loadImage(imgItem, imageview, false);
            if(selectItem.contains(imgItem)){
                stateView.setVisibility(View.VISIBLE);
                layout.setBackgroundResource(R.drawable.bg_border);
            }else {
                stateView.setVisibility(View.GONE);
                layout.setBackgroundResource(R.drawable.grid_item_selector);
            }
            return convertView;
        }
    }

    private void androidViewImage(String path) {
        File file=new File(path);
        Intent it =new Intent(Intent.ACTION_VIEW);
        Uri mUri = Uri.parse("file://"+file.getPath());
        it.setDataAndType(mUri, "image/*");
        startActivity(it);
    }
    /**
     * 拍照事件
     */
    View.OnClickListener photoClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            String filename = getPhoto();
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY,1);
            File out = new File(filename);
            Uri uri = Uri.fromFile(out);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(intent, CAMERA_WITH_DATA);
        }
    };



    private String getPhoto(){
        imagefile = System.currentTimeMillis() + ".jpg";
        if(ToolsUtil.BuldeDirectionary(ConstOptions.IMAGE_STORGE)) {
            imagefile = ConstOptions.IMAGE_STORGE+imagefile;
            return imagefile;
        }
        return imagefile;
    }

    /**
     * 请求页面返回值处理
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        displayImage();
    }

    /**
     * 显示图片
     */
    private void displayImage(){
        File file = new File(imagefile);
        if(file.exists()) {
            //添加图片处理
            mUrlStrs.add(imagefile);
            adaper.notifyDataSetChanged();
        }
    }

    /**
     * GridView中Item的单击事件
     */
    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            String loadimg = mUrlStrs.get(position);
            ImageView imageView =(ImageView) view.findViewById(R.id.id_img);
            RelativeLayout layout = (RelativeLayout) view.findViewById(R.id.grid_item);
            ImageView imgState = (ImageView)view.findViewById(R.id.scan_select);
            if(isEdit){
                if(selectItem.contains(loadimg)){
                    layout.setBackgroundResource(R.drawable.grid_item_selector);
                    imgState.setVisibility(View.GONE);
                    adaper.delNumber(loadimg);
                }else{
                    layout.setBackgroundResource(R.drawable.bg_border);
                    //imageView.setImageResource(R.drawable.bg_border);    //添加图片(带边框的透明图片)[主要目的就是让该图片带边框]
                    imgState.setVisibility(View.VISIBLE);  //设置图片右上角的对号显示
                    adaper.addNumber(loadimg);    //把该图片添加到adapter的选中状态，防止滚动后就没有在选中状态了。
                }
            }else{
                androidViewImage(loadimg);
            }
        }
    };

    /**
     * GridView中Item的长按事件
     */
    AdapterView.OnItemLongClickListener itemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
            if(!isEdit){
                isEdit = true;
                ll_delete.setVisibility(View.VISIBLE);
                ll_tools.setVisibility(View.GONE);
                ImageView imgView = (ImageView)view.findViewById(R.id.id_img);
                RelativeLayout layout = (RelativeLayout) view.findViewById(R.id.grid_item);
                layout.setBackgroundResource(R.drawable.grid_item_selector);
                ImageView imgState = (ImageView)view.findViewById(R.id.scan_select);
                layout.setBackgroundResource(R.drawable.bg_border);
                imgState.setVisibility(View.VISIBLE);
                adaper.addNumber(mUrlStrs.get(i));
                return  true;
            }
            return false;
        }
    };

    /**
     * 删除监听器事件
     */
    private android.view.View.OnClickListener delClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(selectItem.isEmpty()) {
                Toast.makeText(AddInvoiceActivity.this, "请选择图片", Toast.LENGTH_SHORT).show();
                return ;
            }
            pd_schedule = ProgressDialog.show(AddInvoiceActivity.this, "进度", "正在删除...");
           new AsyncTask<String,Integer,Integer>(){
               @Override
               protected Integer doInBackground(String... strings) {
                   try {
                       for (String img : selectItem) {
                           File file = new File(img);
                           boolean isTrue = file.delete();
                       }
                       return 0;
                   }catch (Exception ex){
                       return -1;
                   }
               }
               @Override
               protected void onPostExecute(Integer result){
                   pd_schedule.dismiss();
                    if(result==0) {
                        isEdit = false;
                        ll_delete.setVisibility(View.GONE);
                        ll_tools.setVisibility(View.VISIBLE);
                        adaper.deletePhoto(selectItem);
                        Toast.makeText(AddInvoiceActivity.this, "删除成功！", Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(AddInvoiceActivity.this, "删除失败", Toast.LENGTH_LONG).show();
                    }

               }
           }.execute("");
        }
    };

    /**
     * 按回退按钮事件
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(isEdit && keyCode == KeyEvent.KEYCODE_BACK){    //点击返回按键
            isEdit = false;
            gv_images.setPadding(0, 0, 0, 0);            //退出编辑转台时候，使gridview全屏显示
            ll_delete.setVisibility(View.GONE);
            ll_tools.setVisibility(View.VISIBLE);
            adaper.clear();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 上载按钮事件
     */
    View.OnClickListener uploadOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View view) {
            if(mUrlStrs==null || mUrlStrs.size()==0){
                Toast.makeText(AddInvoiceActivity.this,"请先拍照",Toast.LENGTH_LONG).show();
                return;
            }

            new AsyncTask<List<String>,Integer,DTInfo>(){
                @Override
                protected void onPreExecute() {
                    pd_schedule = ProgressDialog.show(AddInvoiceActivity.this, "", "正在上传中...");
                }

                @Override
                protected DTInfo doInBackground(List<String>... lists) {
                    List<String> files = lists[0];
                    FolderService service = new FolderService();
                    DTInfo info =  service.addFolder(files);
                    return info;
                }

                @Override
                protected void onPostExecute(DTInfo dtInfo) {
                    pd_schedule.dismiss();
                    if(ConstOption.MESSAGE_SUCCESS.equals(dtInfo.getMsg())){
                        Toast.makeText(AddInvoiceActivity.this,"上传完成！",Toast.LENGTH_LONG).show();
                        finish();
                    }else if(ConstOption.MESSAGE_ERROR.equals(dtInfo.getMsg())){
                        Toast.makeText(AddInvoiceActivity.this,dtInfo.getDetailMsg(),Toast.LENGTH_LONG).show();
                    }
                    super.onPostExecute(dtInfo);
                }
            }.execute(mUrlStrs);

        }
    };

}
