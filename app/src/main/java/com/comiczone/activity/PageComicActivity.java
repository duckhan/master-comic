package com.comiczone.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.comiczone.R;
import com.comiczone.adapter.ChapterAdapter;
import com.comiczone.model.Chapter;
import com.comiczone.model.ComicDatabase;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;

public class PageComicActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView mImgThumb;
    private TextView mTvName;
    private TextView mTvAuthor;
    private TextView mTvTrangThai;
    private TextView mTvTheLoai;
    private TextView mTvDescription;
    private String comicUrl;
    private String tacgia;
    private String theloai;
    private String trangthai;
    private String theodoi;
    private String content;
    private String updatetime;
    private String thumb;
    private String title;
    private ArrayList<Chapter> lstChapter;
    private ArrayList<Chapter> lstChapNormal;
    private RecyclerView mRvChapter;
    private ChapterAdapter adapter;
    private TextView mUpdateTv;
    private TextView mViewTv;
    private Spinner mSpinnerSort;
    private Realm myRealm;
    private Toolbar mToolbar;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_comic);
        initView();
        mToolbar.setTitle(getString(R.string.infoComic));
        mToolbar.setTitleTextColor(Color.WHITE);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        myRealm = Realm.getDefaultInstance();
        Intent intent = getIntent();
        comicUrl = intent.getStringExtra("url");
        ArrayList<String> lstSpinner = new ArrayList<>();
        lstSpinner.add("Sắp xếp");
        lstSpinner.add("Từ trên xuống");
        lstSpinner.add("Từ dưới lên");
        int restore = restoringPreferences();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, lstSpinner);
        mSpinnerSort.setAdapter(spinnerAdapter);
        mSpinnerSort.setSelection(restore);
        mSpinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i != 0) {
                    savingPreferences(i);
                }
                if (i == 2) {
                    adapter = new ChapterAdapter(getApplicationContext(), lstChapNormal);
                    RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), 3);
                    mRvChapter.setLayoutManager(mLayoutManager);
                    mRvChapter.setHasFixedSize(true);
                    mRvChapter.setItemAnimator(new DefaultItemAnimator());
                    adapter.notifyDataSetChanged();
                    mRvChapter.setAdapter(adapter);
                }
                if (i == 1) {
                    adapter = new ChapterAdapter(getApplicationContext(), lstChapter);
                    RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), 3);
                    mRvChapter.setLayoutManager(mLayoutManager);
                    mRvChapter.setHasFixedSize(true);
                    mRvChapter.setItemAnimator(new DefaultItemAnimator());
                    adapter.notifyDataSetChanged();
                    mRvChapter.setAdapter(adapter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        loadBook(comicUrl);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void addToDB(String title, String chapter, String thumb, String view, String url) {

        boolean check = false;
        RealmResults<ComicDatabase> results1 =
                myRealm.where(ComicDatabase.class).findAll();
        for (ComicDatabase c : results1) {
            if (c.getName() != null)
                if (c.getName().equals(title)) {
                    Toast.makeText(this, "Truyện đã có trong mục yêu thích !", Toast.LENGTH_SHORT).show();
                    check = true;
                    Log.e("check:", String.valueOf(check));
                    break;

                }
        }

        if (!check) {
            myRealm.beginTransaction();
            ComicDatabase comic = myRealm.createObject(ComicDatabase.class);
            comic.setName(title);
            comic.setChapter(chapter);
            comic.setThumb(thumb);
            comic.setView(view);
            comic.setUrl(url);
            myRealm.commitTransaction();
            Toast.makeText(this, "Đã lưu truyện " + title + " vào mục yêu thích !", Toast.LENGTH_SHORT).show();
        }
    }

    private void savingPreferences(int i) {
        SharedPreferences pre = getSharedPreferences("sort", MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putInt("sort", i);
        editor.apply();
    }


    private int restoringPreferences() {
        SharedPreferences pre = getSharedPreferences
                ("sort", MODE_PRIVATE);
        return pre.getInt("sort", 2);
    }

    private void loadBook(final String url) {
        lstChapNormal.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
                RequestQueue requestQueue = Volley.newRequestQueue(PageComicActivity.this);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Document document = Jsoup.parse(response);
                        //load list chapter
                        Elements chapx = document.select("div.list-chapter");
                        Elements chap = chapx.select("li.row");
                        Elements aElements = chap.select("a");
                        for (Element element : aElements) {
                            String urlChap = element.attr("href");
                            String nameChap = element.text();
                            lstChapNormal.add(new Chapter(nameChap, urlChap));
                        }

                        for (int i = 0; i < lstChapNormal.size(); i++) {
                            String x1 = lstChapNormal.get(lstChapNormal.size() - (i + 1)).getName();
                            String x2 = lstChapNormal.get(lstChapNormal.size() - (i + 1)).getUrl();
                            lstChapter.add(new Chapter(x1, x2));
                        }
                        //load thumbnail
                        Elements contents = document.select("div.detail-info");
                        Element image = contents.select("img").first();
                        thumb = image.attr("src");
                        Glide.with(getApplicationContext()).load(thumb).into(mImgThumb);

                        //load title
                        Elements tieude = document.select("article#item-detail");
                        Element tit = tieude.select("h1").first();
                        title = tit.text();
                        mTvName.setText(title);
                        //time update
                        Element update = tieude.select("time").first();
                        updatetime = update.text();
                        mUpdateTv.setText(updatetime);

                        Elements author;
                        Element tac;
                        Element trang;
                        Element the;
                        Element view;
                        author = document.select("div.col-xs-8.col-info");
                        tac = author.select("p").get(1);
                        trang = author.select("p").get(3);
                        the = author.select("p").get(5);
                        view = author.select("p").get(7);

                        String test = tac.text().trim();
                        Log.e("TEst=", "'" + test + "'");
                        if (test.equals("Tác giả")) {
                            Log.e("TEst:", "true");
                            tac = author.select("p").get(2);
                            trang = author.select("p").get(4);
                            the = author.select("p").get(6);
                            view = author.select("p").get(8);
                        } else {
                            Log.e("TEst:", "false");
                        }
                        tacgia = tac.text();
                        mTvAuthor.setText(tacgia);

                        trangthai = trang.text();
                        mTvTrangThai.setText(trangthai);

                        theloai = the.text();
                        mTvTheLoai.setText(theloai);

                        theodoi = view.text();
                        mViewTv.setText(theodoi);

                        /*Nội dung*/
                        Elements noidung = document.select("div.detail-content");
                        Element cont = noidung.select("p").first();
                        content = cont.text();
                        mTvDescription.setText(content);
                        mRvChapter.post(new Runnable() {
                            @Override
                            public void run() {
                                int i = restoringPreferences();
                                if (i == 2) {
                                    adapter = new ChapterAdapter(getApplicationContext(), lstChapNormal);
                                    RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), 3);
                                    mRvChapter.setLayoutManager(mLayoutManager);
                                    mRvChapter.setHasFixedSize(true);
                                    mRvChapter.setItemAnimator(new DefaultItemAnimator());
                                    adapter.notifyDataSetChanged();
                                    mRvChapter.setAdapter(adapter);
                                }
                                if (i == 1) {
                                    adapter = new ChapterAdapter(getApplicationContext(), lstChapter);
                                    RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), 3);
                                    mRvChapter.setLayoutManager(mLayoutManager);
                                    mRvChapter.setHasFixedSize(true);
                                    mRvChapter.setItemAnimator(new DefaultItemAnimator());
                                    adapter.notifyDataSetChanged();
                                    mRvChapter.setAdapter(adapter);
                                }
                            }
                        });
                        findViewById(R.id.scrollView).scrollTo(0, 0);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });
                stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                        150000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                requestQueue.add(stringRequest);

            }
        }).start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_Like:
                addToDB(title, lstChapNormal.get(0).getName(), thumb, theodoi, comicUrl);
                break;
            default:
                break;
        }
    }

    private void initView() {
        mImgThumb = findViewById(R.id.imgThumb);
        mTvName = findViewById(R.id.tvName);
        mTvAuthor = findViewById(R.id.tvAuthor);
        mTvTrangThai = findViewById(R.id.tv_status);
        mTvTheLoai = findViewById(R.id.tv_kind);
        lstChapter = new ArrayList<>();
        lstChapNormal = new ArrayList<>();
        mRvChapter = findViewById(R.id.rvChapter);
        mUpdateTv = findViewById(R.id.tv_Update);
        mViewTv = findViewById(R.id.tv_View);
        mSpinnerSort = findViewById(R.id.spinnerSort);
        ImageButton mLikeBtn = findViewById(R.id.btn_Like);
        mLikeBtn.setOnClickListener(this);
        mToolbar = findViewById(R.id.toolbar);
        mTvDescription = findViewById(R.id.tv_description);
    }
}
