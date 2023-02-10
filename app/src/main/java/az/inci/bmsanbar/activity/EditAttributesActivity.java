package az.inci.bmsanbar.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.InvAttribute;

public class EditAttributesActivity extends AppBaseActivity
{

    ListView attributeListView;
    String invCode;
    String invName;
    List<InvAttribute> attributeList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_attributes);

        attributeListView = findViewById(R.id.attribute_list);

        invCode = getIntent().getStringExtra("invCode");
        invName = getIntent().getStringExtra("invName");
        setTitle(invName);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        loadData();
    }

    public void loadData()
    {
        attributeList = getAttributeList();
        if (attributeList != null)
        {
            runOnUiThread(this::updatePage);
        }
    }

    public void updatePage()
    {
        if (attributeList.size() > 0)
        {
            findViewById(R.id.save).setOnClickListener(v -> updateAttributes());
        }
        AttributeAdapter adapter = new AttributeAdapter(this, attributeList);
        attributeListView.setAdapter(adapter);
        attributeListView.setOnItemClickListener((parent, view, position, id) ->
                                                 {
                                                     View dialog = LayoutInflater.from(this)
                                                                                 .inflate(
                                                                                         R.layout.edit_attribute_dialog,
                                                                                         parent,
                                                                                         false);
                                                     InvAttribute attribute = attributeList.get(
                                                             position);
                                                     showEditAttributeDialog(attribute, dialog);
                                                 });
    }

    private void showEditAttributeDialog(InvAttribute attribute, View dialog)
    {
        TextView nameText = dialog.findViewById(R.id.attribute_name);
        EditText valueEdit = dialog.findViewById(R.id.attribute_value);
        nameText.setText(attribute.getAttributeName());
        valueEdit.setText(attribute.getAttributeValue());
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(dialog)
                .setPositiveButton("OK", (dialog1, which) ->
                {
                    attribute.setAttributeValue(valueEdit.getText().toString());
                    updatePage();
                })
                .create();
        alertDialog.show();
    }

    private List<InvAttribute> getAttributeList()
    {
        String url = url("inv", "attribute-list-by-whs");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("inv-code", invCode);
        parameters.put("user-id", config().getUser().getId());
        url = addRequestParameters(url, parameters);
        return getListData(url, "GET", null, InvAttribute[].class);
    }

    private void updateAttributes()
    {
        showProgressDialog(true);
        String url = url("inv", "update-attributes");
        executeUpdate(url, attributeList, message ->
        {
            finish();
            showMessageDialog(
                    message.getTitle(),
                    message.getBody(),
                    message.getIconId());
        });
    }

    private static class AttributeAdapter extends ArrayAdapter<InvAttribute>
    {
        Context context;
        List<InvAttribute> attributeList;

        public AttributeAdapter(@NonNull Context context, @NonNull List<InvAttribute> objects)
        {
            super(context, 0, objects);
            this.context = context;
            attributeList = objects;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
        {
            InvAttribute attribute = attributeList.get(position);
            if (convertView == null)
            {
                convertView = LayoutInflater.from(context)
                                            .inflate(R.layout.attribute_item, parent, false);
            }

            ViewHolder holder = new ViewHolder();
            holder.nameEdit = convertView.findViewById(R.id.attribute_name);
            holder.valueCheck = convertView.findViewById(R.id.attribute_value_check);
            holder.valueEdit = convertView.findViewById(R.id.attribute_value);
            holder.nameEdit.setText(attribute.getAttributeName());

            if (attribute.getAttributeType().equals("BIT"))
            {
                holder.valueCheck.setChecked(attribute.getAttributeValue().equals("1"));
                holder.valueCheck.setOnCheckedChangeListener((buttonView, isChecked) ->
                                                                     attribute.setAttributeValue(
                                                                             String.valueOf(
                                                                                     isChecked ? 1 : 0)));
                holder.valueCheck.setVisibility(View.VISIBLE);
                holder.valueEdit.setVisibility(View.GONE);
            }
            else
            {
                holder.valueEdit.setText(attribute.getAttributeValue());
                holder.valueEdit.setVisibility(View.VISIBLE);
                holder.valueCheck.setVisibility(View.GONE);
            }

            if (((EditAttributesActivity) context).config().getUser().isLocation() &&
                (attribute.getAttributeId().equals("AT010")
                 || attribute.getAttributeId().equals("AT011"))
                || !((EditAttributesActivity) context).config().getUser().isLocation())
            {
                convertView.setVisibility(View.VISIBLE);
            }
            else
            {
                convertView.setVisibility(View.GONE);
            }

            return convertView;
        }

        private static class ViewHolder
        {
            TextView nameEdit;
            TextView valueEdit;
            CheckBox valueCheck;
        }
    }
}