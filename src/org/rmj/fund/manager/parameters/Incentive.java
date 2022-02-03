package org.rmj.fund.manager.parameters;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.fund.manager.base.LMasDetTrans;

/**
 * @author Michael Cuison
 * @since October 26, 2021
 */
public class Incentive {
    private final String MASTER_TABLE = "Incentive";
    
    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    private String p_sBranchCd;
    private int p_nEditMode;

    private String p_sMessage;
    private boolean p_bWithUI = true;
    
    private LMasDetTrans p_oListener;
    
    private CachedRowSet p_oRecord;
    
    public Incentive(GRider foApp, String fsBranchCd, boolean fbWithParent){
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;        
                
        if (p_sBranchCd.isEmpty()) p_sBranchCd = p_oApp.getBranchCode();
                
        p_nEditMode = EditMode.UNKNOWN;
    }
    
    public void setListener(LMasDetTrans foValue){
        p_oListener = foValue;
    }
    
    public void setWithUI(boolean fbValue){
        p_bWithUI = fbValue;
    }
    
    public int getEditMode(){
        return p_nEditMode;
    }
    
    public String getMessage(){
        return p_sMessage;
    }
    
    public boolean NewRecord() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        initRecord();

        p_nEditMode = EditMode.ADDNEW;
        return true;
    }
    
    public boolean SaveRecord() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        if (p_nEditMode != EditMode.ADDNEW &&
            p_nEditMode != EditMode.UPDATE){
            p_sMessage = "Invalid edit mode detected.";
            return false;
        }
        
        if (!isEntryOK()) return false;
        
        p_oRecord.updateObject("sModified", p_oApp.getUserID());
        p_oRecord.updateObject("dModified", p_oApp.getServerDate());
        p_oRecord.updateRow();
        
        String lsSQL;
        
        if (p_nEditMode == EditMode.ADDNEW){           
            lsSQL = MiscUtil.getNextCode(MASTER_TABLE, "sInctveCD", false, p_oApp.getConnection(), "");
            p_oRecord.updateObject("sInctveCD", lsSQL);
            p_oRecord.updateRow();
            
            lsSQL = MiscUtil.rowset2SQL(p_oRecord, MASTER_TABLE, "");
        } else {            
            lsSQL = MiscUtil.rowset2SQL(p_oRecord, 
                                        MASTER_TABLE, 
                                        "", 
                                        "sInctveCD = " + SQLUtil.toSQL(p_oRecord.getString("sInctveCD")));
        }
        
        if (!lsSQL.isEmpty()){
            if (!p_bWithParent) p_oApp.beginTrans();
            
            if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
                if (!p_bWithParent) p_oApp.rollbackTrans();
                p_sMessage = p_oApp.getErrMsg() + ";" + p_oApp.getMessage();
                return false;
            }
            
            if (!p_bWithParent) p_oApp.commitTrans();
            
            return true;
        } else
            p_sMessage = "No record to update.";
        
        p_nEditMode = EditMode.UNKNOWN;
        return false;
    }
    
    public boolean ActivateRecord() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid edit mode.";
            return false;
        }
        
        p_oRecord.first();
        
        if ("1".equals(p_oRecord.getString("cRecdStat"))){
            p_sMessage = "Record is active.";
            return false;
        }
        
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                            "  cRecdStat = '1'" +
                            ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sInctveCD = " + SQLUtil.toSQL(p_oRecord.getString("sInctveCD"));

        if (!p_bWithParent) p_oApp.beginTrans();
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
            if (!p_bWithParent) p_oApp.rollbackTrans();
            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
            return false;
        }
        if (!p_bWithParent) p_oApp.commitTrans();
        
        if (p_oListener != null) p_oListener.MasterRetreive(6, "1");
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public boolean DeactivateRecord() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid edit mode.";
            return false;
        }
        
        p_oRecord.first();
        
        if ("0".equals(p_oRecord.getString("cRecdStat"))){
            p_sMessage = "Record is inactive.";
            return false;
        }
        
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                            "  cRecdStat = '0'" +
                            ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sInctveCD = " + SQLUtil.toSQL(p_oRecord.getString("sInctveCD"));

        if (!p_bWithParent) p_oApp.beginTrans();
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
            if (!p_bWithParent) p_oApp.rollbackTrans();
            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
            return false;
        }
        if (!p_bWithParent) p_oApp.commitTrans();
        
        if (p_oListener != null) p_oListener.MasterRetreive(6, "0");
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public boolean SearchRecord(String fsValue, boolean fbByCode) throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        String lsSQL = getSQ_Record();
        
        if (p_bWithUI){
            JSONObject loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "Code»Description", 
                                "sInctveCD»sInctveDs", 
                                "sInctveCD»sInctveDs", 
                                fbByCode ? 0 : 1);
            
            if (loJSON != null) 
                return OpenRecord((String) loJSON.get("sInctveCD"));
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sInctveCD = " + SQLUtil.toSQL(fsValue));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "sInctveDs LIKE " + SQLUtil.toSQL(fsValue + "%")); 
            lsSQL += " LIMIT 1";
        }
        
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        if (!loRS.next()){
            MiscUtil.close(loRS);
            p_sMessage = "No transaction found for the givern criteria.";
            return false;
        }
        
        lsSQL = loRS.getString("sInctveCD");
        MiscUtil.close(loRS);
        
        return OpenRecord(lsSQL);
    }
    
    public boolean OpenRecord(String fsValue) throws SQLException{
        p_nEditMode = EditMode.UNKNOWN;
        
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        
        //open master
        lsSQL = MiscUtil.addCondition(getSQ_Record(), "sInctveCD= " + SQLUtil.toSQL(fsValue));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oRecord = factory.createCachedRowSet();
        p_oRecord.populate(loRS);
        MiscUtil.close(loRS);
        
        p_nEditMode = EditMode.READY;
        return true;
    }
    
    public boolean UpdateRecord() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid edit mode.";
            return false;
        }
        
        p_nEditMode = EditMode.UPDATE;
        return true;
    }
    
    public Object getMaster(int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oRecord.first();
        return p_oRecord.getObject(fnIndex);
    }
    
    public Object getMaster(String fsIndex) throws SQLException{
        return getMaster(getColumnIndex(fsIndex));
    }
    
    public void setMaster(int fnIndex, Object foValue) throws SQLException{
        p_oRecord.first();
        switch (fnIndex){
            case 2://sInctveDs
            case 3://sDivision
            case 4://cInctveTp
            case 5://cByPercnt
                p_oRecord.updateString(fnIndex, ((String) foValue).trim());
                p_oRecord.updateRow();

                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oRecord.getString(fnIndex));
                break;
        }
    }
    
    public void setMaster(String fsIndex, Object foValue) throws SQLException{        
        setMaster(getColumnIndex(fsIndex), foValue);
    }

    public void displayMasFields() throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) return;
        
        int lnRow = p_oRecord.getMetaData().getColumnCount();
        
        System.out.println("----------------------------------------");
        System.out.println("MASTER TABLE INFO");
        System.out.println("----------------------------------------");
        System.out.println("Total number of columns: " + lnRow);
        System.out.println("----------------------------------------");
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            System.out.println("Column index: " + (lnCtr) + " --> Label: " + p_oRecord.getMetaData().getColumnLabel(lnCtr));
            if (p_oRecord.getMetaData().getColumnType(lnCtr) == Types.CHAR ||
                p_oRecord.getMetaData().getColumnType(lnCtr) == Types.VARCHAR){
                
                System.out.println("Column index: " + (lnCtr) + " --> Size: " + p_oRecord.getMetaData().getColumnDisplaySize(lnCtr));
            }
        }
        
        System.out.println("----------------------------------------");
        System.out.println("END: MASTER TABLE INFO");
        System.out.println("----------------------------------------");
        System.out.println("----------------------------------------");
        System.out.println("FIELD VALUES");
        System.out.println("----------------------------------------");
        System.out.println("sDivision --> Multiple select is allowed.");
        System.out.println("0 - Mobile Phone");
        System.out.println("1 - Motorcycle");
        System.out.println("2 - Auto Group - Honda Cars");
        System.out.println("3 - Hospitality");
        System.out.println("4 - Pedritos Group");
        System.out.println("5 - Auto Group - Nissan");
        System.out.println("----------------------------------------");
        System.out.println("cInctveTp --> Single selection only.");
        System.out.println("0 - Branch");
        System.out.println("1 - Main Office");
        System.out.println("2 - Both");
        System.out.println("----------------------------------------");
        System.out.println("cByPercnt --> Single selection only.");
        System.out.println("0 - No");
        System.out.println("1 - Yes");
        System.out.println("2 - Both");
        System.out.println("----------------------------------------");
        System.out.println("END: FIELD VALUES");
        System.out.println("----------------------------------------");
    }
    
    private int getColumnIndex(String fsValue) throws SQLException{
        int lnIndex = 0;
        int lnRow = p_oRecord.getMetaData().getColumnCount();
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            if (fsValue.equals(p_oRecord.getMetaData().getColumnLabel(lnCtr))){
                lnIndex = lnCtr;
                break;
            }
        }
        
        return lnIndex;
    }
    
    private boolean isEntryOK() throws SQLException{
        p_oRecord.first();
        
        if (p_oRecord.getString("sInctveDs").isEmpty()){
            p_sMessage = "Incentive description must not be empty.";
            return false;
        }
        
        if (p_oRecord.getString("sDivision").isEmpty()){
            p_sMessage = "No division selected for this incentive..";
            return false;
        }
        
        if (p_oRecord.getString("cInctveTp").isEmpty()){
            p_sMessage = "Incentive type is not set.";
            return false;
        }
        
        if (p_oRecord.getString("cByPercnt").isEmpty()){
            p_sMessage = "Incentive allocation type is not set.";
            return false;
        }
        
        return true;
    }
    
    private void initRecord() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(8);

        meta.setColumnName(1, "sInctveCD");
        meta.setColumnLabel(1, "sInctveCD");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 3);
        
        meta.setColumnName(2, "sInctveDs");
        meta.setColumnLabel(2, "sInctveDs");
        meta.setColumnType(2, Types.VARCHAR);
        meta.setColumnDisplaySize(2, 32);
        
        meta.setColumnName(3, "sDivision");
        meta.setColumnLabel(3, "sDivision");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 6);
        
        meta.setColumnName(4, "cInctveTp");
        meta.setColumnLabel(4, "cInctveTp");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 1);
        
        meta.setColumnName(5, "cByPercnt");
        meta.setColumnLabel(5, "cByPercnt");
        meta.setColumnType(5, Types.VARCHAR);
        meta.setColumnDisplaySize(5, 1);
        
        meta.setColumnName(6, "cRecdStat");
        meta.setColumnLabel(6, "cRecdStat");
        meta.setColumnType(6, Types.VARCHAR);
        meta.setColumnDisplaySize(6, 1);
        
        meta.setColumnName(7, "sModified");
        meta.setColumnLabel(7, "sModified");
        meta.setColumnType(7, Types.VARCHAR);
        meta.setColumnDisplaySize(7, 12);
        
        meta.setColumnName(8, "dModified");
        meta.setColumnLabel(8, "dModified");
        meta.setColumnType(8, Types.DATE);
        
        p_oRecord = new CachedRowSetImpl();
        p_oRecord.setMetaData(meta);
        
        p_oRecord.last();
        p_oRecord.moveToInsertRow();
        
        MiscUtil.initRowSet(p_oRecord);    
        
        p_oRecord.updateString("sInctveCD", MiscUtil.getNextCode(MASTER_TABLE, "sInctveCD", false, p_oApp.getConnection(), ""));
        p_oRecord.updateString("cInctveTp", "0");
        p_oRecord.updateString("cByPercnt", "0");
        p_oRecord.updateString("cRecdStat", RecordStatus.ACTIVE);
        
        p_oRecord.insertRow();
        p_oRecord.moveToCurrentRow();
    }
    
    private String getSQ_Record(){
        return "SELECT" +
                    "  sInctveCD" +
                    ", sInctveDs" +	
                    ", sDivision" +
                    ", cInctveTp" +
                    ", cByPercnt" +
                    ", cRecdStat" +
                    ", sModified" +	
                    ", dModified" +
                " FROM " + MASTER_TABLE;
    }
}
