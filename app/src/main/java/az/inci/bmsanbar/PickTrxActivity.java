package az.inci.bmsanbar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PickTrxActivity extends ScannerSupportActivity implements SearchView.OnQueryTextListener {

    List<Trx> trxList;
    ListView trxListView;
    ImageButton sendButton;
    ImageButton barcodeButton;
    ImageButton equateAll;
    ImageButton reload;
    SearchView searchView;
    CheckBox continuousCheck;
    CheckBox readyCheck;

    String trxNo;
    String prevTrxNo;
    String pickGroup;
    String pickArea;
    int focusPosition;
    boolean onFocus;
    boolean pickedAll;
    private DecimalFormat decimalFormat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_trx_layout);
        decimalFormat=new DecimalFormat();
        decimalFormat.setGroupingUsed(false);

        sendButton=findViewById(R.id.send);
        barcodeButton=findViewById(R.id.barcode);
        trxListView=findViewById(R.id.trx_list);
        equateAll=findViewById(R.id.equate_all);
        reload =findViewById(R.id.reload);
        continuousCheck=findViewById(R.id.continuous_check);
        readyCheck =findViewById(R.id.readyToSend);

        trxListView.setItemsCanFocus(true);
        sendButton.setEnabled(false);
        sendButton.setBackgroundColor(getResources().getColor(R.color.colorZeroQty));

        continuousCheck.setOnCheckedChangeListener((compoundButton, b) -> isContinuous=b);

        readyCheck.setOnCheckedChangeListener((compoundButton, b) ->
                {
                    sendButton.setEnabled(b);
                    sendButton.setBackgroundColor(b ? Color.GREEN : getResources().getColor(R.color.colorZeroQty));
                }
        );

        if (model.equals("C4000_6582"))
            barcodeButton.setVisibility(View.GONE);

        loadFooter();

        Intent intent=getIntent();
        trxNo=intent.getStringExtra("trxNo");
        prevTrxNo=intent.getStringExtra("prevTrxNo");
        pickGroup=intent.getStringExtra("pickGroup");
        pickArea=intent.getStringExtra("pickArea");
        setTitle(prevTrxNo+" - "+pickArea);

        trxListView.setOnItemClickListener((parent, view, position, id) -> {
            onFocus=true;
            Trx trx= (Trx) view.getTag();
            showEditPickedQtyDialog(trx);
        });

        trxListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Trx trx= (Trx) view.getTag();
            String info=trx.getNotes().replaceAll("; ", "\n");
            info=info.replaceAll("\\\\n", "\n");
            info+="\n\nÖlçü vahidi: "+trx.getUom();
            info+="\n\nBrend: "+trx.getInvBrand();
            info+="\n\nBarkodlar:"+dbHelper.barcodeList(trx.getInvCode(), DBHelper.PICK_TRX);
            AlertDialog.Builder builder=new AlertDialog.Builder(PickTrxActivity.this);
            builder.setTitle("Məlumat");
            builder.setMessage(info);
            builder.setPositiveButton("Şəkil", (dialog, which) -> {
                Intent photoIntent = new Intent(PickTrxActivity.this, PhotoActivity.class);
                photoIntent.putExtra("invCode", trx.getInvCode());
                photoIntent.putExtra("notes", trx.getNotes());
                startActivity(photoIntent);
            });
            builder.setNeutralButton("Say", (dialog, which) -> {
                String url=url("inv","qty",trx.getWhsCode(),trx.getInvCode());
                new ShowQuantity(PickTrxActivity.this).execute(url);
            });
            builder.show();
            return true;
        });

        sendButton.setOnClickListener(v -> {
            if (trxList.size()>0) {
                if (pickedAll)
                    new SendTrx(this).execute();
                else
                {
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setMessage("Mallar tam yığılmayıb. Göndərmək istəyirsiniz?")
                            .setNegativeButton("Bəli", (dialogInterface, i) -> new SendTrx(this).execute())
                            .setPositiveButton("Xeyr", null)
                            .create();

                    dialog.show();
                    playSound(SOUND_FAIL);
                }
            }
        });

        barcodeButton.setOnClickListener(v -> {
            Intent barcodeIntent=new Intent(PickTrxActivity.this, BarcodeScannerCamera.class);
            barcodeIntent.putExtra("serialScan", isContinuous);
            barcodeIntent.putExtra("trxNo", trxNo);
            barcodeIntent.putExtra("trxType", "pick");
            startActivityForResult(barcodeIntent, 1);
        });

        equateAll.setOnClickListener(v -> {
            for (Trx trx : trxList)
            {
                trx.setPickedQty(trx.getQty());
                dbHelper.updatePickTrx(trx);
            }

            loadTrx();
        });

        reload.setOnClickListener(view -> {
            for (Trx trx : trxList)
            {
                trx.setPickedQty(0);
                dbHelper.updatePickTrx(trx);

                loadTrx();
            }
        });

        loadTrx();
    }

    @Override
    public void onScanComplete(String barcode) {
        busy=false;
        Trx trx=dbHelper.getPickTrxByBarcode(barcode, trxNo);
        if (trx!=null) {
            onFocus = true;
            focusPosition=trxList.indexOf(trx);
            if (trx.getPickedQty() >= trx.getQty())
            {
                showMessageDialog(getString(R.string.info),
                        getString(R.string.already_picked),
                        android.R.drawable.ic_dialog_info);
                playSound(SOUND_FAIL);
            }
            else
            {
                if (!isContinuous) {
                    showEditPickedQtyDialog(trx);
                }
                else
                {
                    trx.setPickedQty(trx.getPickedQty() + 1);
                    dbHelper.updatePickTrx(trx);
                }
                playSound(SOUND_SUCCESS);
            }
        }
        else {
            showMessageDialog(getString(R.string.info),
                    getString(R.string.good_not_found),
                    android.R.drawable.ic_dialog_info);
            playSound(SOUND_FAIL);
        }

        loadTrx();
    }

    public void loadTrx()
    {
        pickedAll =true;
        trxList=dbHelper.getPickTrx(trxNo);
        TrxAdapter trxAdapter = new TrxAdapter(this, R.layout.pick_trx_item_layout, trxList);
        trxListView.setAdapter(trxAdapter);
        trxListView.setSelection(focusPosition);
        trxListView.requestFocus();
    }

    public void showEditPickedQtyDialog(Trx trx)
    {
        focusPosition = trxList.indexOf(trx);
        View view=getLayoutInflater()
                .inflate(R.layout.edit_picked_qty_dialog_layout,
                        findViewById(android.R.id.content), false);


        TextView invCodeView=view.findViewById(R.id.inv_code);
        TextView invNameView=view.findViewById(R.id.inv_name);
        EditText qtyEdit=view.findViewById(R.id.qty);
        EditText pickedQtyEdit=view.findViewById(R.id.picked_qty);

        invCodeView.setText(trx.getInvCode());
        invNameView.setText(trx.getInvName());

        qtyEdit.setText(decimalFormat.format(trx.getQty()));
        qtyEdit.setEnabled(false);

        pickedQtyEdit.setText(decimalFormat.format(trx.getPickedQty()));
        pickedQtyEdit.selectAll();

        AlertDialog dialog =new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(R.string.ok, (dialog1, which) -> {
                    double pickedQty;
                    if (!pickedQtyEdit.getText().toString().isEmpty())
                        pickedQty = Double.parseDouble(pickedQtyEdit.getText().toString());
                    else
                        pickedQty=-1;

                    if (pickedQty<0 || pickedQty>trx.getQty())
                    {
                        Toast.makeText(this, R.string.quantity_not_correct, Toast.LENGTH_LONG).show();
                        showEditPickedQtyDialog(trx);
                    }
                    else
                    {
                        trx.setPickedQty(pickedQty);
                        dbHelper.updatePickTrx(trx);
                        loadTrx();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog1, which) -> dialog1.dismiss())
                .setNeutralButton(R.string.equate, (dialog1, which) -> {
                    trx.setPickedQty(trx.getQty());
                    dbHelper.updatePickTrx(trx);
                    loadTrx();
                })
                .create();

        Objects.requireNonNull(dialog.getWindow())
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setActivated(true);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        TrxAdapter adapter= (TrxAdapter) trxListView.getAdapter();
        if (adapter!=null)
            adapter.getFilter().filter(newText);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified())
            searchView.setIconified(true);
        else
            super.onBackPressed();
    }

    static class TrxAdapter extends ArrayAdapter<Trx> implements Filterable
    {
        PickTrxActivity activity;
        List<Trx> list;

        TrxAdapter(@NonNull Context context, int resourceId, @NonNull List<Trx> objects) {
            super(context, resourceId, objects);
            list=objects;
            activity= (PickTrxActivity) context;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Trx trx=list.get(position);
            if (convertView==null)
            {
                convertView= LayoutInflater.from(getContext())
                        .inflate(R.layout.pick_trx_item_layout, parent, false);
            }

            if (position==activity.focusPosition && activity.onFocus) {
                convertView.setBackgroundColor(Color.LTGRAY);
            }
            else {
                convertView.setBackgroundColor(Color.TRANSPARENT);
            }

            if (trx.getPickedQty()==0)
                convertView.setBackgroundColor(activity.getResources().getColor(R.color.colorZeroQty));
            else if (trx.getPickedQty()<trx.getQty())
                convertView.setBackgroundColor(Color.YELLOW);

            TextView invCode=convertView.findViewById(R.id.inv_code);
            TextView invName=convertView.findViewById(R.id.inv_name);
            TextView invBrand=convertView.findViewById(R.id.inv_brand);
            TextView qty=convertView.findViewById(R.id.qty);
            TextView pickedQty=convertView.findViewById(R.id.picked);

            invCode.setText(trx.getInvCode());
            invName.setText(trx.getInvName());
            invBrand.setText(trx.getInvBrand());
            qty.setText(activity.decimalFormat.format(trx.getQty()));
            pickedQty.setText(activity.decimalFormat.format(trx.getPickedQty()));
            convertView.setTag(trx);

            if (trx.getPickedQty() < trx.getQty())
                activity.pickedAll =false;

            return convertView;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    List<Trx> filteredArrayData=new ArrayList<>();
                    constraint=constraint.toString().toLowerCase();

                    for (Trx trx: activity.trxList)
                    {
                        if (trx.getInvCode().concat(trx.getInvName()).concat(trx.getBarcode())
                                .toLowerCase().contains(constraint))
                        {
                            filteredArrayData.add(trx);
                        }
                    }

                    results.count=filteredArrayData.size();
                    results.values=filteredArrayData;
                    return results;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    list= (List<Trx>) results.values;
                    notifyDataSetChanged();
                }
            };
        }
    }

    private static class SendTrx extends AsyncTask<Void, Void, Boolean>
    {
        WeakReference<PickTrxActivity> reference;
        List<Trx> trxList;

        public SendTrx(PickTrxActivity activity)
        {
            reference=new WeakReference<>(activity);
            trxList=reference.get().trxList;
        }

        @Override
        protected void onPreExecute() {
            reference.get().showProgressDialog(true);
        }

        @Override
        protected Boolean doInBackground(Void... aVoid) {
            PickTrxActivity activity=reference.get();
            RestTemplate template=new RestTemplate();
            ((SimpleClientHttpRequestFactory)template.getRequestFactory()).setConnectTimeout(activity.config().getConnectionTimeout()*1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            boolean result = false;
            for (Object item : trxList)
            {
                Trx trx=(Trx)item;
                String url=activity.url("trx",
                        String.valueOf(trx.getTrxId()),
                        String.valueOf(trx.getPickedQty()), "A", null);
                try {
                    result = template.postForObject(url, null, Boolean.class);
                }
                catch (ResourceAccessException ex)
                {
                    ex.printStackTrace();
                    break;
                }
                catch (RuntimeException ex)
                {
                    ex.printStackTrace();
                    return null;
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            PickTrxActivity activity=reference.get();
            if (!result)
            {
                activity.showMessageDialog(activity.getString(R.string.error),
                        activity.getString(R.string.connection_error),
                        android.R.drawable.ic_dialog_alert);
                activity.playSound(SOUND_FAIL);
            }
            else
            {
                activity.dbHelper.deletePickTrx(activity.trxNo);
                activity.finish();
            }
            activity.showProgressDialog(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode!=-1 && data!=null) {
            String barcode=data.getStringExtra("barcode");
            if (barcode != null) {
                onScanComplete(barcode);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrx();
    }
}
