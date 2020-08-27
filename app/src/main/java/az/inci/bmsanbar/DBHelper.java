package az.inci.bmsanbar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DBHelper extends SQLiteOpenHelper {

    public static final String TERMINAL_USER="TERMINAL_USER";
    public static final String PICK_DOC="PICK_DOC";
    public static final String PICK_TRX="PICK_TRX";
    public static final String PACK_DOC="PACK_DOC";
    public static final String PACK_TRX="PACK_TRX";
    public static final String SHIP_TRX="SHIP_TRX";
    public static final String LAST_LOGIN="LAST_LOGIN";
    public static final String CONFIG="CONFIG";

    public static final String USER_ID="USER_ID";
    public static final String USER_NAME="USER_NAME";
    public static final String PASS_WORD="PASS_WORD";
    public static final String COLLECT_FLAG="COLLECT_FLAG";
    public static final String PICK_FLAG="PICK_FLAG";
    public static final String CHECK_FLAG="CHECK_FLAG";
    public static final String COUNT_FLAG="COUNT_FLAG";
    public static final String LOCATION_FLAG="LOCATION_FLAG";
    public static final String PACK_FLAG="PACK_FLAG";
    public static final String DOC_FLAG="DOC_FLAG";
    public static final String LOADING_FLAG="LOADING_FLAG";

    public static final String TRX_ID="TRX_ID";
    public static final String TRX_NO="TRX_NO";
    public static final String TRX_DATE="TRX_DATE";
    public static final String PICK_STATUS="PICK_STATUS";
    public static final String INV_CODE="INV_CODE";
    public static final String INV_NAME="INV_NAME";
    public static final String BRAND_CODE="BRAND_CODE";
    public static final String BP_CODE="BP_CODE";
    public static final String SBE_CODE="SBE_CODE";
    public static final String BP_NAME="BP_NAME";
    public static final String SBE_NAME="SBE_NAME";
    public static final String WHS_CODE="WHS_CODE";
    public static final String UOM="UOM";
    public static final String UOM_FACTOR="UOM_FACTOR";
    public static final String QTY="QTY";
    public static final String PICKED_QTY="PICKED_QTY";
    public static final String PICK_AREA="PICK_AREA";
    public static final String PICK_GROUP="PICK_GROUP";
    public static final String PICK_USER="PICK_USER";
    public static final String APPROVE_USER="APPROVE_USER";
    public static final String BARCODE="BARCODE";
    public static final String PREV_TRX_NO="PREV_TRX_NO";
    public static final String NOTES="NOTES";
    public static final String PRIORITY="PRIORITY";
    public static final String DOC_DESC="DOC_DESC";
    public static final String ITEM_COUNT = "ITEM_COUNT";
    public static final String PICKED_ITEM_COUNT = "PICKED_ITEM_COUNT";
    public static final String REC_STATUS = "REC_STATUS";
    public static final String PACKED_QTY = "PACKED_QTY";
    public static final String REGION_CODE = "REGION_CODE";
    public static final String SRC_TRX_NO = "SRC_TRX_NO";
    public static final String VEHICLE_CODE = "VEHICLE_CODE";
    public static final String DRIVER_CODE = "DRIVER_CODE";
    public static final String NAME = "NAME";
    public static final String VALUE = "VALUE";

    private SQLiteDatabase db;

    DBHelper(Context context) {
        super(context, Objects.requireNonNull(context.getExternalFilesDir("/"))
                .getPath()+"/"+ AppConfig.DB_NAME, null, AppConfig.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createUserTable(db);
        createPickDocTable(db);
        createPickTrxTable(db);
        createPackDocTable(db);
        createPackTrxTable(db);
        createShipTrxTable(db);
        createLastLoginTable(db);
        createConfigTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    void open() throws SQLException
    {
        db=getWritableDatabase();
    }

    @Override
    public synchronized void close() {
        super.close();
    }

    private void createUserTable(SQLiteDatabase db)
    {
        StringBuilder sb=new StringBuilder();

        db.execSQL("DROP TABLE IF EXISTS "+TERMINAL_USER);

        db.execSQL(sb.append("CREATE TABLE ")
                .append(TERMINAL_USER).append("(")
                .append(USER_ID).append(" TEXT,")
                .append(USER_NAME).append(" TEXT,")
                .append(PASS_WORD).append(" TEXT,")
                .append(PICK_GROUP).append(" TEXT,")
                .append(COLLECT_FLAG).append(" INTEGER,")
                .append(PICK_FLAG).append(" INTEGER,")
                .append(CHECK_FLAG).append(" INTEGER,")
                .append(COUNT_FLAG).append(" INTEGER,")
                .append(LOCATION_FLAG).append(" INTEGER,")
                .append(PACK_FLAG).append(" INTEGER,")
                .append(DOC_FLAG).append(" INTEGER,")
                .append(LOADING_FLAG).append(" INTEGER")
                .append(")")
                .toString());
    }

    void addUser(User user)
    {
        db.delete(TERMINAL_USER, USER_ID+"=?", new String[]{user.getId()});

        ContentValues values=new ContentValues();
        values.put(USER_ID, user.getId());
        values.put(USER_NAME, user.getName());
        values.put(PASS_WORD, user.getPassword());
        values.put(PICK_GROUP, user.getPickGroup());
        values.put(COLLECT_FLAG, user.isCollect() ? 1 : 0);
        values.put(PICK_FLAG, user.isPick() ? 1 : 0);
        values.put(CHECK_FLAG, user.isCheck() ? 1 : 0);
        values.put(COUNT_FLAG, user.isCount() ? 1 : 0);
        values.put(LOCATION_FLAG, user.isLocation() ? 1 : 0);
        values.put(PACK_FLAG, user.isPack() ? 1 : 0);
        values.put(DOC_FLAG, user.isDoc() ? 1 : 0);
        values.put(LOADING_FLAG, user.isLoading() ? 1 : 0);

        db.insert(TERMINAL_USER,null, values);
    }

    User getUser(String id)
    {
        String[] columns=new String[]{USER_ID, USER_NAME, PASS_WORD, PICK_GROUP, COLLECT_FLAG,
                PICK_FLAG, CHECK_FLAG, COUNT_FLAG, LOCATION_FLAG, PACK_FLAG, DOC_FLAG, LOADING_FLAG};
        User user = null;

        try (Cursor cursor = db.query(TERMINAL_USER, columns,
                "USER_ID=?", new String[]{id.toUpperCase()}, null, null, null)) {
            if (cursor.moveToNext()) {
                user=new User();
                user.setId(cursor.getString(0));
                user.setName(cursor.getString(1));
                user.setPassword(cursor.getString(2));
                user.setPickGroup(cursor.getString(3));
                user.setCollectFlag(cursor.getInt(4) == 1);
                user.setPickFlag(cursor.getInt(5) == 1);
                user.setCheckFlag(cursor.getInt(6) == 1);
                user.setCountFlag(cursor.getInt(7) == 1);
                user.setLocationFlag(cursor.getInt(8) == 1);
                user.setPackFlag(cursor.getInt(9) == 1);
                user.setDocFlag(cursor.getInt(10) == 1);
                user.setLoadingFlag(cursor.getInt(11) == 1);
            }
        }
        return user;
    }

    private void createPickDocTable(SQLiteDatabase db)
    {
        db.execSQL("DROP TABLE IF EXISTS "+PICK_DOC);

        StringBuilder sb=new StringBuilder();
        try {

            db.execSQL(sb.append("CREATE TABLE ")
                    .append(PICK_DOC).append("(")
                    .append(TRX_NO).append(" TEXT,")
                    .append(TRX_DATE).append(" TEXT,")
                    .append(ITEM_COUNT).append(" INTEGER,")
                    .append(PICK_GROUP).append(" TEXT,")
                    .append(PICK_AREA).append(" TEXT,")
                    .append(DOC_DESC).append(" TEXT,")
                    .append(WHS_CODE).append(" TEXT,")
                    .append(PICK_USER).append(" TEXT,")
                    .append(PICK_STATUS).append(" TEXT,")
                    .append(REC_STATUS).append(" INTEGER,")
                    .append(PREV_TRX_NO).append(" TEXT")
                    .append(")")
                    .toString());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    List<Doc> getPickDocsByPickUser(String pickUser)
    {
        List<Doc> docList=new ArrayList<>();

        String query="SELECT PD.TRX_NO," +
                "PD.TRX_DATE," +
                "PT_ITEM.ITEM_COUNT," +
                "PT_PICKED_ITEM.ITEM_COUNT," +
                "PD.DOC_DESC,"+
                "PD.PREV_TRX_NO,"+
                "PD.PICK_USER,"+
                "PD.PICK_AREA,"+
                "PD.PICK_GROUP "+
                " FROM PICK_DOC PD " +
                "LEFT JOIN (SELECT TRX_NO, COUNT(DISTINCT TRX_ID) ITEM_COUNT " +
                "FROM PICK_TRX GROUP BY TRX_NO) PT_ITEM ON PD.TRX_NO=PT_ITEM.TRX_NO " +
                "LEFT JOIN (SELECT TRX_NO, COUNT(DISTINCT TRX_ID) ITEM_COUNT " +
                "FROM PICK_TRX WHERE PICKED_QTY>0 GROUP BY TRX_NO) PT_PICKED_ITEM " +
                "ON PD.TRX_NO=PT_PICKED_ITEM.TRX_NO WHERE PD.PICK_USER=?";

        try
        {
            Cursor cursor = db.rawQuery(query, new String[]{pickUser});
            while (cursor.moveToNext()) {
                Doc doc = new Doc();
                doc.setTrxNo(cursor.getString(0));
                doc.setTrxDate(cursor.getString(1));
                doc.setItemCount(cursor.getInt(2));
                doc.setPickedItemCount(cursor.getInt(3));
                doc.setDescription(cursor.getString(4));
                doc.setPrevTrxNo(cursor.getString(5));
                doc.setPickUser(cursor.getString(6));
                doc.setPickArea(cursor.getString(7));
                doc.setPickGroup(cursor.getString(8));

                docList.add(doc);
            }
            cursor.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return docList;
    }

    void addPickDoc(Doc doc)
    {
        ContentValues values=new ContentValues();
        values.put(TRX_NO, doc.getTrxNo());
        values.put(TRX_DATE, doc.getTrxDate());
        values.put(ITEM_COUNT, doc.getItemCount());
        values.put(PICK_GROUP, doc.getPickGroup());
        values.put(PICK_AREA, doc.getPickArea());
        values.put(DOC_DESC, doc.getDescription());
        values.put(WHS_CODE, doc.getWhsCode());
        values.put(REC_STATUS, doc.getRecStatus());
        values.put(PICK_USER, doc.getPickUser());
        values.put(PICK_STATUS, doc.getPickStatus());
        values.put(PREV_TRX_NO, doc.getPrevTrxNo());

        db.insert(PICK_DOC,null, values);
    }

    private void createPickTrxTable(SQLiteDatabase db)
    {
        db.execSQL("DROP TABLE IF EXISTS "+PICK_TRX);
        StringBuilder sb=new StringBuilder();

        db.execSQL(sb.append("CREATE TABLE ")
                .append(PICK_TRX).append("(")
                .append(TRX_ID).append(" INTEGER,")
                .append(TRX_NO).append(" TEXT,")
                .append(TRX_DATE).append(" TEXT,")
                .append(PICK_STATUS).append(" TEXT,")
                .append(INV_CODE).append(" TEXT,")
                .append(INV_NAME).append(" TEXT,")
                .append(BRAND_CODE).append(" TEXT,")
                .append(BP_NAME).append(" TEXT,")
                .append(SBE_NAME).append(" TEXT,")
                .append(WHS_CODE).append(" TEXT,")
                .append(UOM).append(" TEXT,")
                .append(UOM_FACTOR).append(" REAL,")
                .append(QTY).append(" REAL,")
                .append(PICKED_QTY).append(" REAL,")
                .append(PICK_AREA).append(" TEXT,")
                .append(PICK_GROUP).append(" TEXT,")
                .append(PICK_USER).append(" TEXT,")
                .append(APPROVE_USER).append(" TEXT,")
                .append(BARCODE).append(" TEXT,")
                .append(PREV_TRX_NO).append(" TEXT,")
                .append(NOTES).append(" TEXT,")
                .append(PRIORITY).append(" INTEGER")
                .append(")")
                .toString());
    }

    void addPickTrx(Trx trx)
    {
        ContentValues values=new ContentValues();
        values.put(TRX_ID, trx.getTrxId());
        values.put(TRX_NO, trx.getTrxNo());
        values.put(TRX_DATE, trx.getTrxDate());
        values.put(PICK_STATUS, trx.getPickStatus());
        values.put(INV_CODE, trx.getInvCode());
        values.put(INV_NAME, trx.getInvName());
        values.put(QTY, trx.getQty());
        values.put(PICKED_QTY, trx.getPickedQty());
        values.put(WHS_CODE, trx.getWhsCode());
        values.put(PICK_AREA, trx.getPickArea());
        values.put(PICK_GROUP, trx.getPickGroup());
        values.put(PICK_USER, trx.getPickUser());
        values.put(APPROVE_USER, trx.getApproveUser());
        values.put(UOM, trx.getUom());
        values.put(UOM_FACTOR, trx.getUomFactor());
        values.put(BARCODE, trx.getBarcode());
        values.put(BP_NAME, trx.getBpName());
        values.put(SBE_NAME, trx.getSbeName());
        values.put(PREV_TRX_NO, trx.getPrevTrxNo());
        values.put(BRAND_CODE, trx.getInvBrand());
        values.put(PRIORITY, trx.getPriority());
        values.put(NOTES, trx.getNotes());

        db.insert(PICK_TRX, null, values);
    }

    List<Trx> getPickTrx(String trxNo) {

        List<Trx> trxList=new ArrayList<>();

        String sql="SELECT * FROM PICK_TRX WHERE TRX_NO=? ORDER BY INV_NAME";

        Cursor cursor = db.rawQuery(sql, new String[]{trxNo});

        int position=0;

        while (cursor.moveToNext())
        {
            Trx trx=new Trx();
            trx.setTrxId(cursor.getInt(0));
            trx.setTrxNo(cursor.getString(1));
            trx.setTrxDate(cursor.getString(2));
            trx.setPickStatus(cursor.getString(3));
            trx.setInvCode(cursor.getString(4));
            trx.setInvName(cursor.getString(5));
            trx.setInvBrand(cursor.getString(6));
            trx.setBpName(cursor.getString(7));
            trx.setSbeName(cursor.getString(8));
            trx.setWhsCode(cursor.getString(9));
            trx.setUom(cursor.getString(10));
            trx.setUomFactor(cursor.getDouble(11));
            trx.setQty(cursor.getDouble(12));
            trx.setPickedQty(cursor.getDouble(13));
            trx.setPickArea(cursor.getString(14));
            trx.setPickGroup(cursor.getString(15));
            trx.setPickUser(cursor.getString(16));
            trx.setApproveUser(cursor.getString(17));
            trx.setBarcode(cursor.getString(18));
            trx.setPrevTrxNo(cursor.getString(19));
            trx.setNotes(cursor.getString(20));
            trx.setPriority(cursor.getInt(21));
            trx.setPosition(position);

            if (!trxList.contains(trx)) {
                trxList.add(trx);
                position++;
            }
        }
        cursor.close();
        return trxList;
    }

    Trx getPickTrxByBarcode(String barcode, String trxNo) {

        String sql="SELECT * FROM PICK_TRX WHERE BARCODE=? AND TRX_NO=?";

        Cursor cursor = db.rawQuery(sql, new String[]{barcode, trxNo});

        Trx trx = null;

        if (cursor.moveToFirst())
        {
            trx = new Trx();
            trx.setTrxId(cursor.getInt(0));
            trx.setTrxNo(cursor.getString(1));
            trx.setTrxDate(cursor.getString(2));
            trx.setPickStatus(cursor.getString(3));
            trx.setInvCode(cursor.getString(4));
            trx.setInvName(cursor.getString(5));
            trx.setInvBrand(cursor.getString(6));
            trx.setBpName(cursor.getString(7));
            trx.setSbeName(cursor.getString(8));
            trx.setWhsCode(cursor.getString(9));
            trx.setUom(cursor.getString(10));
            trx.setUomFactor(cursor.getDouble(11));
            trx.setQty(cursor.getDouble(12));
            trx.setPickedQty(cursor.getDouble(13));
            trx.setPickArea(cursor.getString(14));
            trx.setPickGroup(cursor.getString(15));
            trx.setPickUser(cursor.getString(16));
            trx.setApproveUser(cursor.getString(17));
            trx.setBarcode(cursor.getString(18));
            trx.setPrevTrxNo(cursor.getString(19));
            trx.setNotes(cursor.getString(20));
            trx.setPriority(cursor.getInt(21));
        }
        cursor.close();
        return trx;
    }

    public void updatePickTrx(Trx trx)
    {
        ContentValues values=new ContentValues();
        values.put(PICKED_QTY, trx.getPickedQty());

        db.update(PICK_TRX, values, TRX_ID+"=?", new String[]{String.valueOf(trx.getTrxId())});
    }

    public void deletePickTrx(String trxNo)
    {
        db.delete(PICK_TRX, TRX_NO+"=?", new String[]{trxNo});
        db.delete(PICK_DOC, TRX_NO+"=?", new String[]{trxNo});
    }

    private void createPackDocTable(SQLiteDatabase db)
    {
        db.execSQL("DROP TABLE IF EXISTS "+PACK_DOC);

        StringBuilder sb=new StringBuilder();
        try {

            db.execSQL(sb.append("CREATE TABLE ")
                    .append(PACK_DOC).append("(")
                    .append(TRX_NO).append(" TEXT,")
                    .append(TRX_DATE).append(" TEXT,")
                    .append(ITEM_COUNT).append(" INTEGER,")
                    .append(PICKED_ITEM_COUNT).append(" INTEGER,")
                    .append(PICK_GROUP).append(" TEXT,")
                    .append(PICK_AREA).append(" TEXT,")
                    .append(DOC_DESC).append(" TEXT,")
                    .append(WHS_CODE).append(" TEXT,")
                    .append(PICK_USER).append(" TEXT,")
                    .append(PICK_STATUS).append(" TEXT,")
                    .append(REC_STATUS).append(" INTEGER,")
                    .append(PREV_TRX_NO).append(" TEXT,")
                    .append(BP_CODE).append(" TEXT,")
                    .append(BP_NAME).append(" TEXT,")
                    .append(SBE_CODE).append(" TEXT,")
                    .append(SBE_NAME).append(" TEXT,")
                    .append(APPROVE_USER).append(" TEXT,")
                    .append(NOTES).append(" TEXT")
                    .append(")")
                    .toString());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    List<Doc> getPackDocsByApproveUser(String approveUser)
    {
        List<Doc> docList=new ArrayList<>();

        String query="SELECT PD.TRX_NO," +
                "PD.TRX_DATE," +
                "PT_ITEM.ITEM_COUNT," +
                "PT_PICKED_ITEM.ITEM_COUNT," +
                "PD.DOC_DESC,"+
                "PD.PREV_TRX_NO,"+
                "PD.BP_CODE,"+
                "PD.BP_NAME,"+
                "PD.SBE_CODE,"+
                "PD.SBE_NAME,"+
                "PD.APPROVE_USER,"+
                "PD.NOTES"+
                " FROM PACK_DOC PD " +
                "LEFT JOIN (SELECT TRX_NO, COUNT(DISTINCT TRX_ID) ITEM_COUNT " +
                    "FROM PACK_TRX GROUP BY TRX_NO) PT_ITEM ON PD.TRX_NO=PT_ITEM.TRX_NO " +
                "LEFT JOIN (SELECT TRX_NO, COUNT(DISTINCT TRX_ID) ITEM_COUNT " +
                    "FROM PACK_TRX WHERE PICKED_QTY>0 GROUP BY TRX_NO) PT_PICKED_ITEM " +
                "ON PD.TRX_NO=PT_PICKED_ITEM.TRX_NO WHERE PD.APPROVE_USER=? ORDER BY PD.PREV_TRX_NO";

        try
        {
            Cursor cursor = db.rawQuery(query, new String[]{approveUser});
            while (cursor.moveToNext()) {
                Doc doc = new Doc();
                doc.setTrxNo(cursor.getString(0));
                doc.setTrxDate(cursor.getString(1));
                doc.setItemCount(cursor.getInt(2));
                doc.setPickedItemCount(cursor.getInt(3));
                doc.setDescription(cursor.getString(4));
                doc.setPrevTrxNo(cursor.getString(5));
                doc.setBpCode(cursor.getString(6));
                doc.setBpName(cursor.getString(7));
                doc.setSbeCode(cursor.getString(8));
                doc.setSbeName(cursor.getString(9));
                doc.setApproveUser(cursor.getString(10));
                doc.setNotes(cursor.getString(11));

                docList.add(doc);
            }
            cursor.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return docList;
    }

    void addPackDoc(Doc doc)
    {
        ContentValues values=new ContentValues();
        values.put(TRX_NO, doc.getTrxNo());
        values.put(TRX_DATE, doc.getTrxDate());
        values.put(ITEM_COUNT, doc.getItemCount());
        values.put(PICKED_ITEM_COUNT, doc.getPickedItemCount());
        values.put(PICK_GROUP, doc.getPickGroup());
        values.put(PICK_AREA, doc.getPickArea());
        values.put(DOC_DESC, doc.getDescription());
        values.put(WHS_CODE, doc.getWhsCode());
        values.put(REC_STATUS, doc.getRecStatus());
        values.put(PICK_USER, doc.getPickUser());
        values.put(PICK_STATUS, doc.getPickStatus());
        values.put(PREV_TRX_NO, doc.getPrevTrxNo());
        values.put(BP_CODE, doc.getBpCode());
        values.put(BP_NAME, doc.getBpName());
        values.put(SBE_CODE, doc.getSbeCode());
        values.put(SBE_NAME, doc.getSbeName());
        values.put(APPROVE_USER, doc.getApproveUser());
        values.put(NOTES, doc.getNotes());

        db.delete(PACK_DOC, TRX_NO+"=?", new String[]{doc.getTrxNo()});

        db.insert(PACK_DOC,null, values);
    }

    private void createPackTrxTable(SQLiteDatabase db)
    {
        db.execSQL("DROP TABLE IF EXISTS "+PACK_TRX);
        StringBuilder sb=new StringBuilder();

        db.execSQL(sb.append("CREATE TABLE ")
                .append(PACK_TRX).append("(")
                .append(TRX_ID).append(" INTEGER,")
                .append(TRX_NO).append(" TEXT,")
                .append(TRX_DATE).append(" TEXT,")
                .append(PICK_STATUS).append(" TEXT,")
                .append(INV_CODE).append(" TEXT,")
                .append(INV_NAME).append(" TEXT,")
                .append(BRAND_CODE).append(" TEXT,")
                .append(BP_NAME).append(" TEXT,")
                .append(SBE_NAME).append(" TEXT,")
                .append(WHS_CODE).append(" TEXT,")
                .append(UOM).append(" TEXT,")
                .append(UOM_FACTOR).append(" REAL,")
                .append(QTY).append(" REAL,")
                .append(PICKED_QTY).append(" REAL,")
                .append(PACKED_QTY).append(" REAL,")
                .append(PICK_AREA).append(" TEXT,")
                .append(PICK_GROUP).append(" TEXT,")
                .append(PICK_USER).append(" TEXT,")
                .append(APPROVE_USER).append(" TEXT,")
                .append(BARCODE).append(" TEXT,")
                .append(PREV_TRX_NO).append(" TEXT,")
                .append(NOTES).append(" TEXT,")
                .append(PRIORITY).append(" INTEGER")
                .append(")")
                .toString());
    }

    void addPackTrx(Trx trx)
    {
        ContentValues values=new ContentValues();
        values.put(TRX_ID, trx.getTrxId());
        values.put(TRX_NO, trx.getTrxNo());
        values.put(TRX_DATE, trx.getTrxDate());
        values.put(PICK_STATUS, trx.getPickStatus());
        values.put(INV_CODE, trx.getInvCode());
        values.put(INV_NAME, trx.getInvName());
        values.put(QTY, trx.getQty());
        values.put(PICKED_QTY, trx.getPickedQty());
        values.put(PACKED_QTY, trx.getPackedQty());
        values.put(WHS_CODE, trx.getWhsCode());
        values.put(PICK_AREA, trx.getPickArea());
        values.put(PICK_GROUP, trx.getPickGroup());
        values.put(PICK_USER, trx.getPickUser());
        values.put(APPROVE_USER, trx.getApproveUser());
        values.put(UOM, trx.getUom());
        values.put(UOM_FACTOR, trx.getUomFactor());
        values.put(BARCODE, trx.getBarcode());
        values.put(BP_NAME, trx.getBpName());
        values.put(SBE_NAME, trx.getSbeName());
        values.put(PREV_TRX_NO, trx.getPrevTrxNo());
        values.put(BRAND_CODE, trx.getInvBrand());
        values.put(PRIORITY, trx.getPriority());
        values.put(NOTES, trx.getNotes());

        db.insert(PACK_TRX, null, values);
    }

    List<Trx> getPackTrxByApproveUser(String trxNo) {

        List<Trx> trxList=new ArrayList<>();

        String sql="SELECT * FROM PACK_TRX WHERE TRX_NO=? ORDER BY INV_NAME";

        Cursor cursor = db.rawQuery(sql, new String[]{trxNo});

        int position=0;

        while (cursor.moveToNext())
        {
            Trx trx=new Trx();
            trx.setTrxId(cursor.getInt(0));
            trx.setTrxNo(cursor.getString(1));
            trx.setTrxDate(cursor.getString(2));
            trx.setPickStatus(cursor.getString(3));
            trx.setInvCode(cursor.getString(4));
            trx.setInvName(cursor.getString(5));
            trx.setInvBrand(cursor.getString(6));
            trx.setBpName(cursor.getString(7));
            trx.setSbeName(cursor.getString(8));
            trx.setWhsCode(cursor.getString(9));
            trx.setUom(cursor.getString(10));
            trx.setUomFactor(cursor.getDouble(11));
            trx.setQty(cursor.getDouble(12));
            trx.setPickedQty(cursor.getDouble(13));
            trx.setPackedQty(cursor.getDouble(14));
            trx.setPickArea(cursor.getString(15));
            trx.setPickGroup(cursor.getString(16));
            trx.setPickUser(cursor.getString(17));
            trx.setApproveUser(cursor.getString(18));
            trx.setBarcode(cursor.getString(19));
            trx.setPrevTrxNo(cursor.getString(20));
            trx.setNotes(cursor.getString(21));
            trx.setPriority(cursor.getInt(22));
            trx.setPosition(position);

            if (!trxList.contains(trx)) {
                trxList.add(trx);
                position++;
            }
        }
        cursor.close();
        return trxList;
    }

    Trx getPackTrxByBarcode(String barcode, String trxNo) {

        String sql="SELECT * FROM PACK_TRX WHERE BARCODE=? AND TRX_NO=?";

        Cursor cursor = db.rawQuery(sql, new String[]{barcode, trxNo});

        Trx trx = null;

        if (cursor.moveToFirst())
        {
            trx = new Trx();
            trx.setTrxId(cursor.getInt(0));
            trx.setTrxNo(cursor.getString(1));
            trx.setTrxDate(cursor.getString(2));
            trx.setPickStatus(cursor.getString(3));
            trx.setInvCode(cursor.getString(4));
            trx.setInvName(cursor.getString(5));
            trx.setInvBrand(cursor.getString(6));
            trx.setBpName(cursor.getString(7));
            trx.setSbeName(cursor.getString(8));
            trx.setWhsCode(cursor.getString(9));
            trx.setUom(cursor.getString(10));
            trx.setUomFactor(cursor.getDouble(11));
            trx.setQty(cursor.getDouble(12));
            trx.setPickedQty(cursor.getDouble(13));
            trx.setPackedQty(cursor.getDouble(14));
            trx.setPickArea(cursor.getString(15));
            trx.setPickGroup(cursor.getString(16));
            trx.setPickUser(cursor.getString(17));
            trx.setApproveUser(cursor.getString(18));
            trx.setBarcode(cursor.getString(19));
            trx.setPrevTrxNo(cursor.getString(20));
            trx.setNotes(cursor.getString(21));
            trx.setPriority(cursor.getInt(22));
        }
        cursor.close();
        return trx;
    }

    public void updatePackTrx(Trx trx)
    {
        ContentValues values=new ContentValues();
        values.put(PACKED_QTY, trx.getPackedQty());

        db.update(PACK_TRX, values, TRX_ID+"=?", new String[]{String.valueOf(trx.getTrxId())});
    }

    public void deletePackTrx(String trxNo)
    {
        db.delete(PACK_TRX, TRX_NO+"=?", new String[]{trxNo});
        db.delete(PACK_DOC, TRX_NO+"=?", new String[]{trxNo});
    }

    public void createShipTrxTable(SQLiteDatabase db)
    {
        db.execSQL("DROP TABLE IF EXISTS "+SHIP_TRX);
        StringBuilder sb=new StringBuilder();

        db.execSQL(sb.append("CREATE TABLE ")
                .append(SHIP_TRX).append("(")
                .append(REGION_CODE).append(" TEXT,")
                .append(DRIVER_CODE).append(" TEXT,")
                .append(SRC_TRX_NO).append(" TEXT,")
                .append(VEHICLE_CODE).append(" TEXT,")
                .append(USER_ID).append(" TEXT")
                .append(")")
                .toString());
    }

    public void addShipTrx(ShipTrx shipTrx)
    {
        ContentValues values=new ContentValues();

        values.put(REGION_CODE, shipTrx.getRegionCode());
        values.put(DRIVER_CODE, shipTrx.getDriverCode());
        values.put(SRC_TRX_NO, shipTrx.getSrcTrxNo());
        values.put(VEHICLE_CODE, shipTrx.getVehicleCode());
        values.put(USER_ID, shipTrx.getUserId());

        db.insert(SHIP_TRX, null, values);
    }

    public List<ShipDoc> getShipDocs(String userId)
    {
        List<ShipDoc> shipDocList=new ArrayList<>();
        ShipDoc doc;

        Cursor cursor = db.rawQuery("SELECT REGION_CODE, DRIVER_CODE, VEHICLE_CODE, USER_ID, COUNT(*)" +
                        " FROM SHIP_TRX WHERE USER_ID=? GROUP BY REGION_CODE, DRIVER_CODE, VEHICLE_CODE, USER_ID",
                new String[]{userId});
        while (cursor.moveToNext())
        {
            doc=new ShipDoc();
            doc.setRegionCode(cursor.getString(0));
            doc.setDriverCode(cursor.getString(1));
            doc.setVehicleCode(cursor.getString(2));
            doc.setUserId(cursor.getString(3));
            doc.setCount(cursor.getInt(4));
            shipDocList.add(doc);
        }
        cursor.close();

        return shipDocList;
    }

    public List<ShipTrx> getShipTrx(String driver)
    {
        List<ShipTrx> shipTrxList=new ArrayList<>();
        ShipTrx trx;

        Cursor cursor = db.rawQuery("SELECT * FROM SHIP_TRX WHERE DRIVER_CODE=?", new String[]{driver});
        while (cursor.moveToNext())
        {
            trx=new ShipTrx();
            trx.setRegionCode(cursor.getString(0));
            trx.setDriverCode(cursor.getString(1));
            trx.setSrcTrxNo(cursor.getString(2));
            trx.setVehicleCode(cursor.getString(3));
            trx.setUserId(cursor.getString(4));
            shipTrxList.add(trx);
        }

        cursor.close();

        return shipTrxList;
    }

    public void deleteShipTrxByDriver(String driverCode)
    {
        db.delete(SHIP_TRX, DRIVER_CODE+"=?", new String[]{driverCode});
    }

    public void deleteShipTrxBySrc(String srcTrxNo)
    {
        db.delete(SHIP_TRX, SRC_TRX_NO+"=?", new String[]{srcTrxNo});
    }

    public boolean isShipped(String trxNo)
    {
        boolean shipped=false;
        Cursor cursor = db.rawQuery("SELECT * FROM SHIP_TRX WHERE SRC_TRX_NO=?", new String[]{trxNo});
        if (cursor.moveToFirst())
            shipped=true;
        cursor.close();
        return shipped;
    }

    public String barcodeList(String invCode, String tableName)
    {
        StringBuilder barcodeList= new StringBuilder();
        Cursor cursor = db.query(tableName,
                new String[]{BARCODE},
                INV_CODE+"=?",
                new String[]{invCode},
                BARCODE, null, null, null);
        while (cursor.moveToNext())
            barcodeList.append("\n").append(cursor.getString(0));
        cursor.close();
        return barcodeList.toString();
    }

    private void createLastLoginTable(SQLiteDatabase db)
    {
        db.execSQL("DROP TABLE IF EXISTS "+LAST_LOGIN);
        db.execSQL("CREATE TABLE LAST_LOGIN (USER_ID TEXT, PASS_WORD TEXT)");
    }

    public void updateLastLogin(String userId, String password)
    {
        db.delete(LAST_LOGIN, USER_ID+"=?", new String[]{userId});

        ContentValues values=new ContentValues();
        values.put(USER_ID, userId);
        values.put(PASS_WORD, password);

        db.insert(LAST_LOGIN, null, values);
    }

    public String[] getLastLogin()
    {
        String[] result=new String[2];
        Cursor cursor = db.rawQuery("SELECT * FROM LAST_LOGIN", null);

        if (cursor.moveToLast())
        {
            result[0]=cursor.getString(0);
            result[1]=cursor.getString(1);
        }

        cursor.close();

        return result;
    }

    private void createConfigTable(SQLiteDatabase db)
    {
        db.execSQL("DROP TABLE IF EXISTS "+CONFIG);
        db.execSQL("CREATE TABLE CONFIG (NAME TEXT, VALUE TEXT)");
    }

    public String getParameter(String name)
    {
        String value="";

        Cursor cursor=db.rawQuery("SELECT VALUE FROM CONFIG WHERE NAME=?", new String[]{name});
        if (cursor.moveToFirst())
        {
            value=cursor.getString(0);
        }

        cursor.close();

        return value;
    }

    public void updateParameter(String name, String value)
    {
        ContentValues values=new ContentValues();
        values.put(NAME, name);
        values.put(VALUE, value);
        try {
            db.delete(CONFIG, NAME+"=?", new String[]{name});
            db.insert(CONFIG, null, values);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

    }
}
