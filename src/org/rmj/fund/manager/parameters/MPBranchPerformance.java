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
import org.rmj.appdriver.StringUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.fund.manager.base.LMasDetTrans;

/**
 * @author Michael Cuison
 * @since November 23, 2021
 */
public class MPBranchPerformance {
    private String MASTER_TABLE = "MP_Branch_Performance";
    
    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    private String p_sBranchCd;
    private int p_nEditMode;

    private String p_sMessage;
    private boolean p_bWithUI = true;
    
    private LMasDetTrans p_oListener;
    
    private CachedRowSet p_oRecord;
    
    public MPBranchPerformance(GRider foApp, String fsBranchCd, boolean fbWithParent){
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
        
        p_oRecord.updateObject("dModified", p_oApp.getServerDate());
        p_oRecord.updateRow();
        
        String lsSQL;
        
        if (p_nEditMode == EditMode.ADDNEW){                       
            lsSQL = MiscUtil.rowset2SQL(p_oRecord, MASTER_TABLE, "xBranchNm");
            
            if (!lsSQL.isEmpty()){
                lsSQL += " ON DUPLICATE KEY UPDATE" +
                            "  nCPGoalxx = " + getMaster("nCPGoalxx") +
                            ", nCPGoalxx = " + getMaster("nCPGoalxx") +
                            ", nCPGoalxx = " + getMaster("nCPGoalxx") +
                            ", nCPGoalxx = " + getMaster("nCPGoalxx") +
                            ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate());
            }
        } else {            
            lsSQL = MiscUtil.rowset2SQL(p_oRecord, 
                                        MASTER_TABLE, 
                                        "xBranchNm", 
                                        "sBranchCd = " + SQLUtil.toSQL(p_oRecord.getString("sBranchCd")) +
                                        "AND sPeriodxx = " + SQLUtil.toSQL(p_oRecord.getString("sPeriodxx")));
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
            
        //p_nEditMode = EditMode.UNKNOWN;
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
                                "Period»Branch", 
                                "sPeriodxx»xBranchNm", 
                                "a.sPeriodxx»b.sBranchNm", 
                                fbByCode ? 0 : 1);
            
            if (loJSON != null) 
                return OpenRecord((String) loJSON.get("sBranchCd"), 
                                    (String) loJSON.get("sPeriodxx"));
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sPeriodxx = " + SQLUtil.toSQL(fsValue));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "b.sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%")); 
            lsSQL += " ORDER BY a.sPeriodxx DESC LIMIT 1";
        }
        
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        if (!loRS.next()){
            MiscUtil.close(loRS);
            p_sMessage = "No transaction found for the givern criteria.";
            return false;
        }
        
        lsSQL = loRS.getString("sBranchCd");
        p_sMessage = loRS.getString("sPeriodxx");
        MiscUtil.close(loRS);
        
        return OpenRecord(lsSQL, p_sMessage);
    }
    
    public boolean OpenRecord(String fsBranchCd, String fsPeriodxx) throws SQLException{
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
        lsSQL = MiscUtil.addCondition(getSQ_Record(), "a.sBranchCd = " + SQLUtil.toSQL(fsBranchCd) +
                                                        " AND a.sPeriodxx = " + SQLUtil.toSQL(fsPeriodxx));
        
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
            case 1://sBranchCd
            case 2://sPeriodxx
                if (p_nEditMode != EditMode.ADDNEW){
                    if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oRecord.getString(fnIndex));
                
                    break;
                }
            case 8://xBranchNm
                p_oRecord.updateString(fnIndex, ((String) foValue).trim());
                p_oRecord.updateRow();

                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oRecord.getString(fnIndex));
                
                break;
            case 3://nCPGoalxx
            case 5://nCPActual
                p_oRecord.updateInt(fnIndex, 0);

                if (StringUtil.isNumeric(String.valueOf(foValue))) 
                    p_oRecord.updateInt(fnIndex, (int) foValue);
                
                p_oRecord.updateRow();  
                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oRecord.getString(fnIndex));
                
                break;
            case 4://nAcGoalxx
            case 6://nAcActual
                p_oRecord.updateDouble(fnIndex, 0.00);
                
                if (StringUtil.isNumeric(String.valueOf(foValue))) 
                    p_oRecord.updateDouble(fnIndex, (double) foValue);
                
                p_oRecord.updateRow();   
                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oRecord.getString(fnIndex));
                
                break;
        }
    }
    
    public void setMaster(String fsIndex, Object foValue) throws SQLException{        
        setMaster(getColumnIndex(fsIndex), foValue);
    }
    
    public boolean searchBranch(String fsValue, boolean fbByCode) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW){
            p_sMessage = "Changing of branch is not allowed on current edit mode.";
            return false;
        } 
        
        String lsSQL = "SELECT" +
                            "  sBranchCd" +
                            ", sBranchNm" +
                        " FROM Branch" +
                        " WHERE cRecdStat = '1'" +
                            " AND sBranchCd LIKE 'C%'";
        
        JSONObject loJSON;
        
        if (p_bWithUI){
            loJSON = showFXDialog.jsonSearch(
                        p_oApp, 
                        lsSQL, 
                        fsValue, 
                        "Code»Branch", 
                        "sBranchCd»sBranchNm", 
                        "sBranchCd»sBranchNm", 
                        fbByCode ? 0 : 1);
            
            if (loJSON != null){
                p_oRecord.first();
                p_oRecord.updateString("sBranchCd", (String) loJSON.get("sBranchCd"));
                p_oRecord.updateRow();
                
                setMaster("xBranchNm", (String) loJSON.get("sBranchNm"));            

                if (p_oListener != null) p_oListener.MasterRetreive(12, getMaster("xBranchNm"));
                return true;
            } else 
                return false;
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchCd = " + SQLUtil.toSQL(fsValue));
        else
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
        
        lsSQL += " LIMIT 1";
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        JSONArray loArray = MiscUtil.RS2JSON(loRS);
        MiscUtil.close(loRS);
        
        if (loArray.isEmpty()) return false;
        
        loJSON = (JSONObject) loArray.get(0);
        
        if (loJSON != null){
            p_oRecord.first();
            p_oRecord.updateString("sBranchCd", (String) loJSON.get("sBranchCd"));
            p_oRecord.updateRow();

            setMaster("xBranchNm", (String) loJSON.get("sBranchNm"));            

            if (p_oListener != null) p_oListener.MasterRetreive(12, getMaster("xBranchNm"));
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
        
        if (p_oRecord.getString("sBranchCd").isEmpty()){
            p_sMessage = "Branch must not be empty.";
            return false;
        }
        
        if (p_oRecord.getString("sPeriodxx").isEmpty()){
            p_sMessage = "Period must not be empty.";
            return false;
        }
        
        return true;
    }
    
    private void initRecord() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(8);

        meta.setColumnName(1, "sBranchCd");
        meta.setColumnLabel(1, "sBranchCd");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 4);
        
        meta.setColumnName(2, "sPeriodxx");
        meta.setColumnLabel(2, "sPeriodxx");
        meta.setColumnType(2, Types.VARCHAR);
        meta.setColumnDisplaySize(2, 6);
        
        meta.setColumnName(3, "nCPGoalxx");
        meta.setColumnLabel(3, "nCPGoalxx");
        meta.setColumnType(3, Types.INTEGER);
        
        meta.setColumnName(4, "nAcGoalxx");
        meta.setColumnLabel(4, "nAcGoalxx");
        meta.setColumnType(4, Types.DECIMAL);
        
        meta.setColumnName(5, "nCPActual");
        meta.setColumnLabel(5, "nCPActual");
        meta.setColumnType(5, Types.INTEGER);
        
        meta.setColumnName(6, "nAcActual");
        meta.setColumnLabel(6, "nAcActual");
        meta.setColumnType(6, Types.DECIMAL);
        
        meta.setColumnName(7, "dModified");
        meta.setColumnLabel(7, "dModified");
        meta.setColumnType(7, Types.DATE);
        
        meta.setColumnName(8, "xBranchNm");
        meta.setColumnLabel(8, "xBranchNm");
        meta.setColumnType(8, Types.VARCHAR);
        
        p_oRecord = new CachedRowSetImpl();
        p_oRecord.setMetaData(meta);
        
        p_oRecord.last();
        p_oRecord.moveToInsertRow();
        
        MiscUtil.initRowSet(p_oRecord);    
        
        p_oRecord.insertRow();
        p_oRecord.moveToCurrentRow();
    }
    
    private String getSQ_Record(){
        return "SELECT" +
                    "  a.sBranchCd" +
                    ", a.sPeriodxx" +
                    ", a.nCPGoalxx" +
                    ", a.nAcGoalxx" +
                    ", a.nCPActual" +
                    ", a.nAcActual" +
                    ", a.dModified" +
                    ", b.sBranchNm xBranchNm" +
                " FROM " + MASTER_TABLE + " a" +
                    " LEFT JOIN Branch b ON a.sBranchCd = b.sBranchCd"; 
    }
}
