package az.inci.bmsanbar;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditAttributesActivity extends AppBaseActivity {

    ListView attributeListView;
    String invCode;
    String invName;
    List<InvAttribute> attributeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_attributes);

        attributeListView=findViewById(R.id.attribute_list);

        invCode=getIntent().getStringExtra("invCode");
        invName=getIntent().getStringExtra("invName");
        setTitle(invName);

        getData();
    }

    private void getData()
    {
        showProgressDialog(true);
        new Thread(() -> {
            attributeList=getAttributeList();
            runOnUiThread(this::loadData);
        }).start();
    }

    private void loadData()
    {
        showProgressDialog(false);
        AttributeAdapter adapter=new AttributeAdapter(this, attributeList);
        attributeListView.setAdapter(adapter);
    }

    private List<InvAttribute> getAttributeList()
    {
        List<InvAttribute> result;

        String url = url("inv", "attribute-list");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("inv-code", invCode);
        url = addRequestParameters(url, parameters);
        RestTemplate template = new RestTemplate();
        ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                .setConnectTimeout(config().getConnectionTimeout() * 1000);
        template.getMessageConverters().add(new StringHttpMessageConverter());
        try
        {
            String content = template.getForObject(url, String.class);
            Gson gson = new Gson();
            result = new ArrayList<>(gson.fromJson(content, new TypeToken<List<InvAttribute>>(){}.getType()));
        }
        catch (RuntimeException ex)
        {
            ex.printStackTrace();
            return new ArrayList<>();
        }
        return result;
    }

    private static class AttributeAdapter extends ArrayAdapter<InvAttribute>
    {
        Context context;
        List<InvAttribute> attributeList;

        public AttributeAdapter(@NonNull Context context, @NonNull List<InvAttribute> objects) {
            super(context, 0, objects);
            this.context=context;
            attributeList=objects;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            InvAttribute attribute=attributeList.get(position);
            if (convertView==null)
            {
                convertView= LayoutInflater.from(context).inflate(R.layout.attribute_item, parent, false);
            }

            ViewHolder holder=new ViewHolder();

            holder.nameEdit=convertView.findViewById(R.id.attribute_name);
            holder.valueEdit=convertView.findViewById(R.id.attribute_value);
            holder.nameEdit.setText(attribute.getAttributeName());
            holder.valueEdit.setText(attribute.getAttributeValue());

            return convertView;
        }

        private static class ViewHolder
        {
            TextView nameEdit;
            EditText valueEdit;
        }
    }
}