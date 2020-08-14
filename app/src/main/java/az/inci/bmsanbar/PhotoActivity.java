package az.inci.bmsanbar;

import android.os.Bundle;
import android.webkit.WebView;

public class PhotoActivity extends AppBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_info);
        WebView webView=findViewById(R.id.photo_view);
        String invCode=getIntent().getStringExtra("invCode");
        String imgUrl=config().getImageUrl()+"/"+invCode+".jpg";
        String htmlCode="<html><head><style>img {max-width: 100%}" +
                "</style></head><body><img src='"+imgUrl+"'/></body></html>";
        webView.loadData(htmlCode, "text/html", "UTF-8");
    }
}