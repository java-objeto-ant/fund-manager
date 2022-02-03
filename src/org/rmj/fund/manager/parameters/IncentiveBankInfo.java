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
import org.json.simple.JSONArray;
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
public class IncentiveBankInfo {
    private final String MASTER_TABLE = "Employee_Incentive_Bank_Info";
    
    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    private String p_sBranchCd;
    private int p_nEditMode;
    private int p_nRecdStat;

    private String p_sMessage;
    private boolean p_bWithUI = true;
    
    private LMasDetTrans p_oListener;
    
    private CachedRowSet p_oRecord;
    
    public IncentiveBankInfo(GRider foApp, String fsBranchCd, boolean fbWithParent){
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;        
                
        if (p_sBranchCd.isEmpty()) p_sBranchCd = p_oApp.getBranchCode();
                
        p_nRecdStat = 10;
        p_nEditMode = EditMode.UNKNOWN;
    }
    
    public void setRecordStat(int fnValue){
        p_nRecdStat = fnValue;
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
            lsSQL = MiscUtil.rowset2SQL(p_oRecord, MASTER_TABLE, "xEmployNm;xBankName");
        } else {            
            lsSQL = MiscUtil.rowset2SQL(p_oRecord, 
                                        MASTER_TABLE, 
                                        "xEmployNm;xBankName", 
                                        "sEmployID = " + SQLUtil.toSQL(p_oRecord.getString("sEmployID")));
        }
        
        if (!lsSQL.isEmpty()){
            if (!p_bWithParent) p_oApp.beginTrans();
            
            if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
                if (!p_bWithParent) p_oApp.rollbackTrans();
                p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                return false;
            }
            
            if (!p_bWithParent) p_oApp.commitTrans();
            
            return true;
        } else
            p_sMessage = "No record to update.";
        
        p_nEditMode = EditMode.UNKNOWN;
        return false;
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
                                "ID»Employee»Bank»Account No.", 
                                "sEmployID»xEmployNm»xBankName»sBankAcct", 
                                "a.sEmployID»c.sCompnyNm»d.sBankName»a.sBankAcct", 
                                fbByCode ? 0 : 1);
            
            if (loJSON != null) 
                return OpenRecord((String) loJSON.get("sEmployID"));
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sEmployID = " + SQLUtil.toSQL(fsValue));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "c.sCompnyNm LIKE " + SQLUtil.toSQL(fsValue + "%")); 
            lsSQL += " LIMIT 1";
        }
        
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        if (!loRS.next()){
            MiscUtil.close(loRS);
            p_sMessage = "No transaction found for the givern criteria.";
            return false;
        }
        
        lsSQL = loRS.getString("sEmployID");
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
        lsSQL = MiscUtil.addCondition(getSQ_Record(), "a.sEmployID = " + SQLUtil.toSQL(fsValue));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oRecord = factory.createCachedRowSet();
        p_oRecord.populate(loRS);
        MiscUtil.close(loRS);
        
        if (p_oRecord.size() == 0) return false;
        
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
                        " WHERE sEmployID = " + SQLUtil.toSQL(p_oRecord.getString("sEmployID"));

        if (!p_bWithParent) p_oApp.beginTrans();
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
            if (!p_bWithParent) p_oApp.rollbackTrans();
            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
            return false;
        }
        if (!p_bWithParent) p_oApp.commitTrans();
        
        if (p_oListener != null) p_oListener.MasterRetreive(4, "1");
        
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
                        " WHERE sEmployID = " + SQLUtil.toSQL(p_oRecord.getString("sEmployID"));

        if (!p_bWithParent) p_oApp.beginTrans();
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
            if (!p_bWithParent) p_oApp.rollbackTrans();
            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
            return false;
        }
        if (!p_bWithParent) p_oApp.commitTrans();
        
        if (p_oListener != null) p_oListener.MasterRetreive(4, "0");
        
        p_nEditMode = EditMode.UNKNOWN;
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
            case 1: //sEmployID
                searchEmployee((String) foValue, true);
                break;
            case 2: //sBankIDxx
                searchBank((String) foValue, true);
                break;
            case 3: //sBankAcct
                p_oRecord.updateString(fnIndex, ((String) foValue).trim());
                p_oRecord.updateRow();

                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oRecord.getString(fnIndex));
                break;
            default:
                p_oRecord.updateString(fnIndex, ((String) foValue).trim());
                p_oRecord.updateRow();
        }
    }
    
    public void setMaster(String fsIndex, Object foValue) throws SQLException{
        setMaster(getColumnIndex(fsIndex), foValue);
    }
    
    public boolean searchEmployee(String fsValue, boolean fbByCode) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW) {
            p_sMessage = "This feature is for new record only.";
            return false;
        }
        
        String lsSQL = "SELECT" +
                            "  a.sEmployID" +
                            ", b.sCompnyNm" +
                            ", c.sBranchNm" +
                        " FROM Employee_Master001 a" +
                            ", Client_Master b" +
                            ", Branch c" +
                        " WHERE a.sEmployID = b.sClientID" +
                            " AND a.sBranchCd = c.sBranchCd" +
                            " AND a.cRecdStat = '1'" +
                            " AND ISNULL(a.dFiredxxx)";
        
        ResultSet loRS;
        JSONObject loJSON;
        
        if (p_bWithUI){
            loJSON = showFXDialog.jsonSearch(
                        p_oApp, 
                        lsSQL, 
                        fsValue, 
                        "ID»Employee»Branch", 
                        "a.sEmployID»b.sCompnyNm»c.sBranchNm", 
                        "a.sEmployID»b.sCompnyNm»c.sBranchNm", 
                        fbByCode ? 0 : 1);
            
            if (loJSON != null){
                lsSQL = MiscUtil.addCondition(getSQ_Record(), "a.sEmployID = " + SQLUtil.toSQL((String) loJSON.get("sEmployID"))); 
                loRS = p_oApp.executeQuery(lsSQL);
                
                if (loRS.next()){
                    if (OpenRecord((String) loJSON.get("sEmployID"))){
                        if (UpdateRecord()){
                            if (p_oListener != null) p_oListener.MasterRetreive(7, getMaster("xEmployNm"));
                            if (p_oListener != null) p_oListener.MasterRetreive(8, getMaster("xBankName"));
                            if (p_oListener != null) p_oListener.MasterRetreive(3, getMaster("sBankAcct"));
                            return true;
                        }
                    }
                } else {
                    MiscUtil.close(loRS);
                    
                    p_oRecord.first();
                    p_oRecord.updateString("sEmployID", (String) loJSON.get("sEmployID"));
                    p_oRecord.updateRow();

                    setMaster("xEmployNm", (String) loJSON.get("sCompnyNm"));

                    if (p_oListener != null) p_oListener.MasterRetreive(7, getMaster("xEmployNm"));
                    return true;
                }
            }
            
            return false;
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sEmployID = " + SQLUtil.toSQL(fsValue));
        else
            lsSQL = MiscUtil.addCondition(lsSQL, "b.sCompnyNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
        
        lsSQL += " LIMIT 1";
        loRS = p_oApp.executeQuery(lsSQL);
        
        JSONArray loArray = MiscUtil.RS2JSON(loRS);
        MiscUtil.close(loRS);
        
        if (loArray.isEmpty()) return false;
        
        loJSON = (JSONObject) loArray.get(0);
        
        if (loJSON != null){
                lsSQL = MiscUtil.addCondition(getSQ_Record(), "a.sEmployID = " + SQLUtil.toSQL((String) loJSON.get("sEmployID"))); 
                loRS = p_oApp.executeQuery(lsSQL);
                
                if (loRS.next()){
                    if (OpenRecord((String) loJSON.get("sEmployID"))){
                        if (UpdateRecord()){
                            if (p_oListener != null) p_oListener.MasterRetreive(7, getMaster("xEmployNm"));
                            if (p_oListener != null) p_oListener.MasterRetreive(8, getMaster("xBankName"));
                            if (p_oListener != null) p_oListener.MasterRetreive(3, getMaster("sBankAcct"));
                            return true;
                        }
                    }
                } else {
                    MiscUtil.close(loRS);
                    
                    p_oRecord.first();
                    p_oRecord.updateString("sEmployID", (String) loJSON.get("sEmployID"));
                    p_oRecord.updateRow();

                    setMaster("xEmployNm", (String) loJSON.get("sCompnyNm"));

                    if (p_oListener != null) p_oListener.MasterRetreive(7, getMaster("xEmployNm"));
                    return true;
                }
            }
            return false;
    }
    
    public boolean searchBank(String fsValue, boolean fbByCode) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW &&
            p_nEditMode != EditMode.UPDATE) return false;
        
        String lsSQL = "SELECT" +
                            "  sBankIDxx" +
                            ", sBankName" +
                        " FROM Banks" +
                        " WHERE cRecdStat = '1'";
        
        JSONObject loJSON;
        
        if (p_bWithUI){
            loJSON = showFXDialog.jsonSearch(
                        p_oApp, 
                        lsSQL, 
                        fsValue, 
                        "ID»Bank", 
                        "sBankIDxx»sBankName", 
                        "sBankIDxx»sBankName", 
                        fbByCode ? 0 : 1);
            
            if (loJSON != null){
                p_oRecord.first();
                p_oRecord.updateString("sBankIDxx", (String) loJSON.get("sBankIDxx"));
                p_oRecord.updateRow();
                
                setMaster("xBankName", (String) loJSON.get("sBankName"));            

                if (p_oListener != null) p_oListener.MasterRetreive(8, getMaster("xBankName"));
                return true;
            } else 
                return false;
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sBankIDxx = " + SQLUtil.toSQL(fsValue));
        else
            lsSQL = MiscUtil.addCondition(lsSQL, "sBankName LIKE " + SQLUtil.toSQL(fsValue + "%"));
        
        lsSQL += " LIMIT 1";
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        JSONArray loArray = MiscUtil.RS2JSON(loRS);
        MiscUtil.close(loRS);
        
        if (loArray.isEmpty()) return false;
        
        loJSON = (JSONObject) loArray.get(0);
        
        if (loJSON != null){
            p_oRecord.first();
            p_oRecord.updateString("sBankIDxx", (String) loJSON.get("sBankIDxx"));
            p_oRecord.updateRow();

            setMaster("xBankName", (String) loJSON.get("sBankName"));            

            if (p_oListener != null) p_oListener.MasterRetreive(8, getMaster("xBankName"));
            return true;
        } else
            return false;
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
    }
    
    private boolean isEntryOK() throws SQLException{
        p_oRecord.first();
        
        if (p_oRecord.getString("sEmployID").isEmpty()){
            p_sMessage = "Employee is not set.";
            return false;
        }
        
        if (p_oRecord.getString("sBankIDxx").isEmpty()){
            p_sMessage = "No bank was selected.";
            return false;
        }
        
        if (p_oRecord.getString("sBankAcct").isEmpty()){
            p_sMessage = "Bank account number must not be empty.";
            return false;
        }
        
        return true;
    }
    
    private void initRecord() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(8);

        meta.setColumnName(1, "sEmployID");
        meta.setColumnLabel(1, "sEmployID");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);
        
        meta.setColumnName(2, "sBankIDxx");
        meta.setColumnLabel(2, "sBankIDxx");
        meta.setColumnType(2, Types.VARCHAR);
        meta.setColumnDisplaySize(2, 9);
        
        meta.setColumnName(3, "sBankAcct");
        meta.setColumnLabel(3, "sBankAcct");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 16);
        
        meta.setColumnName(4, "cRecdStat");
        meta.setColumnLabel(4, "cRecdStat");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 1);
        
        meta.setColumnName(5, "sModified");
        meta.setColumnLabel(5, "sModified");
        meta.setColumnType(5, Types.VARCHAR);
        meta.setColumnDisplaySize(5, 12);
        
        meta.setColumnName(6, "dModified");
        meta.setColumnLabel(6, "dModified");
        meta.setColumnType(6, Types.DATE);
        
        meta.setColumnName(7, "xEmployNm");
        meta.setColumnLabel(7, "xEmployNm");
        meta.setColumnType(7, Types.VARCHAR);
        
        meta.setColumnName(8, "xBankName");
        meta.setColumnLabel(8, "xBankName");
        meta.setColumnType(8, Types.VARCHAR);
        
        p_oRecord = new CachedRowSetImpl();
        p_oRecord.setMetaData(meta);
        
        p_oRecord.last();
        p_oRecord.moveToInsertRow();
        
        MiscUtil.initRowSet(p_oRecord);       
        p_oRecord.updateString("cRecdStat", RecordStatus.ACTIVE);
        
        p_oRecord.insertRow();
        p_oRecord.moveToCurrentRow();
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
    
    private String getSQ_Record(){
        String lsSQL = "";
        String lsStat = String.valueOf(p_nRecdStat);
        
        if (lsStat.length() > 1){
            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
                lsSQL += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            
            lsSQL = " AND a.cRecdStat IN (" + lsSQL.substring(2) + ")";
        } else 
            lsSQL = " AND a.cRecdStat = " + SQLUtil.toSQL(lsStat);
        
        lsSQL =  "SELECT" +
                    "  a.sEmployID" +
                    ", a.sBankIDxx" +
                    ", a.sBankAcct" +
                    ", a.cRecdStat" +
                    ", a.sModified" +
                    ", a.dModified" +
                    ", c.sCompnyNm xEmployNm" +
                    ", d.sBankName xBankName" +
                " FROM " + MASTER_TABLE + " a" +
                    ", Employee_Master001 b" +
                    ", Client_Master c" +
                    ", Banks d" +
                " WHERE a.sEmployID = b.sEmployID" +
                    " AND b.sEmployID = c.sClientID" +
                    " AND a.sBankIDxx = d.sBankIDxx" +
                    lsSQL;
        
        return lsSQL;
    }
}
