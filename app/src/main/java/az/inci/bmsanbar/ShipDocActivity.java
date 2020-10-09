package az.inci.bmsanbar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class ShipDocActivity extends AppBaseActivity
{

    ListView docListView;
    ImageButton add;

    List<ShipDoc> docList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ship_doc_layout);
        setTitle("Yükləmə sənədləri");

        docListView = findViewById(R.id.ship_doc_list_view);
        add = findViewById(R.id.add);

        add.setOnClickListener(v ->
        {
            Intent intent = new Intent(ShipDocActivity.this, ShipTrxActivity.class);
            intent.putExtra("mode", AppConfig.NEW_MODE);
            startActivity(intent);
        });

        docListView.setOnItemClickListener((parent, view, position, id) ->
        {
            ShipDoc doc = (ShipDoc) parent.getItemAtPosition(position);
            Intent intent = new Intent(ShipDocActivity.this, ShipTrxActivity.class);
            intent.putExtra("driverCode", doc.getDriverCode());
            intent.putExtra("vehicleCode", doc.getVehicleCode());
            startActivity(intent);
        });

        docListView.setOnItemLongClickListener((parent, view, position, id) ->
        {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage("Silmək istəyirsinizmi?")
                    .setPositiveButton("Sil", (dialogInterface, i) ->
                    {
                        ShipDoc doc = (ShipDoc) parent.getItemAtPosition(position);
                        dbHelper.deleteShipTrxByDriver(doc.getDriverCode());
                        loadDocs();
                    })
                    .setNegativeButton("Ləğv et", null)
                    .create();
            dialog.show();
            return true;
        });

        loadDocs();

        loadFooter();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        loadDocs();
    }

    void loadDocs()
    {
        docList = dbHelper.getShipDocs(config().getUser().getId());
        DocAdapter adapter = new DocAdapter(this, R.layout.ship_doc_item_layout, docList);
        docListView.setAdapter(adapter);
    }

    private static class DocAdapter extends ArrayAdapter<ShipDoc>
    {
        public DocAdapter(@NonNull Context context, int resource, @NonNull List<ShipDoc> objects)
        {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
        {
            if (convertView == null)
            {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.ship_doc_item_layout, parent, false);
            }

            ShipDoc doc = getItem(position);

            TextView driverTextView = convertView.findViewById(R.id.driver);
            TextView vehicleTextView = convertView.findViewById(R.id.vehicle);
            TextView countTextView = convertView.findViewById(R.id.count);

            assert doc != null;
            driverTextView.setText(doc.getDriverCode());
            vehicleTextView.setText(doc.getVehicleCode());
            countTextView.setText(String.valueOf(doc.getCount()));

            return convertView;
        }
    }
}
