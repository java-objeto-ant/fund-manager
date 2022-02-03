package org.rmj.fund.manager.base;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.Types;
import java.sql.SQLException;
import java.util.Date;
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
import org.rmj.appdriver.constants.TransactionStatus;

/**
 * @author Michael Cuison
 * @since January 28, 2022
 */
public class CashCount {
    private final String MASTER_TABLE = "Cash_Count_Master";
    private final String RQST_DEPT = "('034', '026')"; //audit
    private final String SALES = "015;036;026"; //sales;mobile phone sales;
    
    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    private String p_sBranchCd;
    private int p_nEditMode;
    private int p_nTranStat;

    private String p_sMessage;
    private boolean p_bWithUI = true;

    private CachedRowSet p_oMaster;
    private LTransaction p_oListener;
   
    public CashCount(GRider foApp, String fsBranchCd, boolean fbWithParent){        
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;        
                
        if (p_sBranchCd.isEmpty()) p_sBranchCd = p_oApp.getBranchCode();
        
        p_nTranStat = 0;
        p_nEditMode = EditMode.UNKNOWN;
    }
    
    public void setTranStat(int fnValue){
        p_nTranStat = fnValue;
    }
   
    public void setListener(LTransaction foValue){
        p_oListener = foValue;
    }
    
    public void setWithUI(boolean fbValue){
        p_bWithUI = fbValue;
    }
    
    public int getEditMode(){
        return p_nEditMode;
    }
    
    public boolean NewTransaction() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        if (!SALES.contains(p_oApp.getDepartment())){
            p_sMessage = "Only SALES department are allowed to entry cash count;";
            return false;
        }
        
        p_sMessage = "";
        
        createMaster();

        p_nEditMode = EditMode.ADDNEW;
        return true;
    }
    
    public boolean SaveTransaction() throws SQLException{
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
        
        int lnCtr;
        String lsSQL;
        
        if (p_nEditMode == EditMode.ADDNEW){            
            //set transaction number on records
            String lsTransNox = MiscUtil.getNextCode(MASTER_TABLE, "sTransNox", true, p_oApp.getConnection(), p_sBranchCd);
            p_oMaster.updateObject("sTransNox", lsTransNox);
            p_oMaster.updateObject("sBranchCd", p_sBranchCd);
            p_oMaster.updateObject("sEntryByx", p_oApp.getEmployeeNo());
            p_oMaster.updateObject("dEntryDte", p_oApp.getServerDate());
            p_oMaster.updateObject("sModified", p_oApp.getUserID());
            p_oMaster.updateObject("dModified", p_oApp.getServerDate());
            p_oMaster.updateRow();
            
            lsSQL = MiscUtil.rowset2SQL(p_oMaster, MASTER_TABLE, "xBranchNm;xEntryByx;xReqstdBy;xReadxxxx");
            
            if (!lsSQL.isEmpty()){
                if (!p_bWithParent) p_oApp.beginTrans();
                if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                    if (!p_bWithParent) p_oApp.rollbackTrans();
                    p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                    return false;
                }
                if (!p_bWithParent) p_oApp.commitTrans();
                
                p_nEditMode = EditMode.UNKNOWN;
                return true;
            } else{
                p_sMessage = "No record to save.";
                return false;
            }
        } else {           
            //set transaction number on records
            String lsTransNox = (String) getMaster("sTransNox");
            
            lsSQL = MiscUtil.rowset2SQL(p_oMaster, 
                                        MASTER_TABLE, 
                                        "xBranchNm;xEntryByx;xReqstdBy;xReadxxxx", 
                                        "sTransNox = " + SQLUtil.toSQL(lsTransNox));
            
            if (!lsSQL.isEmpty()){
                if (!p_bWithParent) p_oApp.beginTrans();
                if (!lsSQL.isEmpty()){
                    if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                        if (!p_bWithParent) p_oApp.rollbackTrans();
                        p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                        return false;
                    }
                }
                if (!p_bWithParent) p_oApp.commitTrans();

                p_nEditMode = EditMode.UNKNOWN;
                return true;
            } else{
                p_sMessage = "No record to save.";
                return false;
            }
        }
    }
    
    public boolean SearchTransaction(String fsValue, boolean fbByCode) throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";    
        
        String lsSQL = getSQ_Master();
        
        if (p_oApp.isMainOffice()){
            if (SALES.contains(p_oApp.getDepartment()))
                lsSQL = MiscUtil.addCondition(lsSQL, "a.sBranchCd = " + SQLUtil.toSQL(p_sBranchCd));
            else if (!RQST_DEPT.contains(p_oApp.getDepartment())){
                p_sMessage = "User is not allowed to search cash count.";
                return false;
            }    
        } else
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sBranchCd = " + SQLUtil.toSQL(p_sBranchCd));
        
        if (p_bWithUI){
            JSONObject loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "Trans. No.»Branch»Date", 
                                "a.sTransNox»xBranchNm»a.dTransact", 
                                "a.sTransNox»b.sBranchNm»a.dTransact", 
                                fbByCode ? 0 : 1);
            
            if (loJSON != null) 
                return OpenTransaction((String) loJSON.get("sTransNox"));
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox = " + SQLUtil.toSQL(fsValue));   
        else {
            if (!fsValue.isEmpty()) {
                lsSQL = MiscUtil.addCondition(lsSQL, "b.sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%")); 
                lsSQL += " LIMIT 1";
            }
        }
        
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        if (!loRS.next()){
            MiscUtil.close(loRS);
            p_sMessage = "No transaction found for the givern criteria.";
            return false;
        }
        
        lsSQL = loRS.getString("sTransNox");
        MiscUtil.close(loRS);
        
        return OpenTransaction(lsSQL);
    }
    
    public boolean OpenTransaction(String fsTransNox) throws SQLException{
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
        lsSQL = MiscUtil.addCondition(getSQ_Master(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oMaster = factory.createCachedRowSet();
        p_oMaster.populate(loRS);
        MiscUtil.close(loRS);
        
        p_oMaster.last();
        if (p_oMaster.getRow() <= 0) {
            p_sMessage = "No transaction was loaded.";
            return false;
        }
        
        if (RQST_DEPT.contains(p_oApp.getDepartment()) && 
            !"1".equals((String) getMaster("cReadxxxx"))) {
            lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                    "  cReadxxxx = '1'" +
                    ", sReadxxxx = " + SQLUtil.toSQL(p_oApp.getEmployeeNo()) +
                    ", dReadxxxx = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                " WHERE sTransNox = " + SQLUtil.toSQL(getMaster("sTransNox"));
            
            if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, (String) getMaster("sBranchCd")) <= 0){
                p_sMessage = "Transaction was not loaded. Unable to update status.";
                return false;
            }
        }
        
        p_nEditMode = EditMode.READY;
        
        return true;
    }
    
    public boolean UpdateTransaction() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid edit mode.";
            return false;
        }
        
        if (p_bWithParent) {
            p_sMessage = "Updating of record from other object is not allowed.";
            return false;
        }
        
        if (!SALES.contains(p_oApp.getDepartment())){
            p_sMessage = "Only SALES department are allowed to entry cash count;";
            return false;
        }
        
        if (Integer.parseInt((String) getMaster("cTranStat")) > 0){
            p_sMessage = "Unable to update processed transactions.";
            return false;
        }
        
        p_nEditMode = EditMode.UPDATE;
        return true;
    }
    
    public boolean CloseTransaction() throws SQLException{        
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid update mode detected.";
            return false;
        }
        
        p_sMessage = "";
        
        if (p_bWithParent) {
            p_sMessage = "Confirming transactions from other object is not allowed.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("1")){            
            p_sMessage = "Transaction was already confirmed.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("2")){
            p_sMessage = "Unable to confirm posted transactions.";
            return false;
        }
        
        String lsTransNox = (String) getMaster("sTransNox");
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                            "  cTranStat = '1'" +
                            ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);
        
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public boolean PostTransaction() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid update mode detected.";
            return false;
        }
        
        p_sMessage = "";
        
        if (((String) getMaster("cTranStat")).equals("2")){
            p_sMessage = "Transaction was already posted..";
            return false;
        }  
        
        if (((String) getMaster("cTranStat")).equals("0")){
            p_sMessage = "Unable to post unconfirmed transactions.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("3")){
            p_sMessage = "Unable to post cancelled transactions.";
            return false;
        }
        
        String lsTransNox = (String) getMaster("sTransNox");
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                            "  cTranStat = '2'" +
                            ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);
        
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public boolean CancelTransaction() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid update mode detected.";
            return false;
        }
        
        p_sMessage = "";
        
        if (p_bWithParent) {
            p_sMessage = "Cancelling transactions from other object is not allowed.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("2")){
            p_sMessage = "Unable to cancel posted transactions.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("3")){
            p_sMessage = "Transaction was already cancelled.";
            return false;
        }
        
        String lsTransNox = (String) getMaster("sTransNox");
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                            "  cTranStat = '3'" +
                            ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);
        
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public Object getMaster(int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oMaster.first();
        return p_oMaster.getObject(fnIndex);
    }
    
    public Object getMaster(String fsIndex) throws SQLException{
        return getMaster(getColumnIndex(p_oMaster, fsIndex));
    }
    
    public void setMaster(int fnIndex, Object foValue) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) {
            System.out.println("Invalid Edit Mode Detected.");
            return;
        }
        
        p_oMaster.first();
        
        switch (fnIndex){
            case 3: //dTransact
                if (foValue instanceof Date){
                    p_oMaster.updateDate(fnIndex, SQLUtil.toDate((Date) foValue));
                } else
                    p_oMaster.updateDate(fnIndex, SQLUtil.toDate(p_oApp.getServerDate()));
                
                p_oMaster.updateRow();
                
                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oMaster.getString(fnIndex));
                break;
            case 25://sReqstdBy
                //search employee
                break;
            case 4: //nCn0001cx
            case 5: //nCn0005cx
            case 6: //nCn0010cx
            case 7: //nCn0025cx
            case 8: //nCn0001px
            case 9: //nCn0005px
            case 10: //nCn0010px
            case 11: //nCn0020px
            case 12: //nNte0020p
            case 13: //nNte0050p
            case 14: //nNte0100p
            case 15: //nNte0200p
            case 16: //nNte0500p
            case 17: //nNte1000p
                if (!StringUtil.isNumeric(String.valueOf(foValue))){
                    if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, 0);
                    break;
                }
                
                p_oMaster.updateInt(fnIndex, (int) foValue);
                p_oMaster.updateRow();
                
                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oMaster.getString(fnIndex));
                break;
            case 18://sORNoxxxx
            case 19://sSINoxxxx
            case 20://sPRNoxxxx
            case 21://sCRNoxxxx
                p_oMaster.updateString(fnIndex, (String) foValue);
                p_oMaster.updateRow();

                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oMaster.getString(fnIndex));
                break;
        }
    }
    
    public void setMaster(String fsIndex, Object foValue) throws SQLException{
        setMaster(getColumnIndex(p_oMaster, fsIndex), foValue);
    }
    
    public boolean searchRequestor(String fsValue, boolean fbByCode) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW) return false;
        
        if (fbByCode)
            if (fsValue.equals((String) getMaster("sReqstdBy"))) return true;
        else
            if (fsValue.equals((String) getMaster("xReqstdBy"))) return true;
        
            
        String lsSQL = "SELECT" +
                            "  a.sEmployID" +
                            ", b.sCompnyNm" +
                            ", IFNULL(c.sDeptName, '') sDeptName" +
                        " FROM Employee_Master001 a" +
                            " LEFT JOIN Client_Master b ON a.sEmployID = b.sClientID" +
                            " LEFT JOIN Department c ON a.sDeptIDxx = c.sDeptIDxx" +
                        " WHERE a.sDeptIDxx IN " + RQST_DEPT +
                            " AND a.cRecdStat = '1'";
        
        JSONObject loJSON;
        
        if (p_bWithUI){
            loJSON = showFXDialog.jsonSearch(
                        p_oApp, 
                        lsSQL, 
                        fsValue, 
                        "ID»Name»Department", 
                        "sEmployID»sCompnyNm»sDeptName", 
                        "a.sEmployID»b.sCompnyNm»IFNULL(c.sDeptName, '')", 
                        fbByCode ? 0 : 1);
            
            if (loJSON != null){
                p_oMaster.updateString("sReqstdBy", (String) loJSON.get("sEmployID"));
                p_oMaster.updateString("xReqstdBy", (String) loJSON.get("sCompnyNm"));
                p_oMaster.updateRow();

                if (p_oListener != null) p_oListener.MasterRetreive(32, getMaster("xReqstdBy"));
                
                return true;
            }
            
            p_oMaster.updateString("sReqstdBy", "");
            p_oMaster.updateString("xReqstdBy", "");
            p_oMaster.updateRow();
            
            if (p_oListener != null) p_oListener.MasterRetreive(32, "");
            
            return false;
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sEmployID = " + SQLUtil.toSQL(fsValue));
        else
            lsSQL = MiscUtil.addCondition(lsSQL, "b.sCompnyNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
        
        lsSQL += " LIMIT 1";
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        JSONArray loArray = MiscUtil.RS2JSON(loRS);
        MiscUtil.close(loRS);
        
        if (loArray.isEmpty()) return false;
        
        loJSON = (JSONObject) loArray.get(0);
        
        p_oMaster.updateString("sReqstdBy", (String) loJSON.get("sEmployID"));
        p_oMaster.updateString("xReqstdBy", (String) loJSON.get("sCompnyNm"));
        p_oMaster.updateRow();

        if (p_oListener != null) p_oListener.MasterRetreive(32, p_oMaster.getString("xReqstdBy"));        
        return true;
    }
   
    public String getMessage(){
        return p_sMessage;
    }
    
    public void displayMasFields() throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) return;
        
        int lnRow = p_oMaster.getMetaData().getColumnCount();
        
        System.out.println("----------------------------------------");
        System.out.println("MASTER TABLE INFO");
        System.out.println("----------------------------------------");
        System.out.println("Total number of columns: " + lnRow);
        System.out.println("----------------------------------------");
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            System.out.println("Column index: " + (lnCtr) + " --> Label: " + p_oMaster.getMetaData().getColumnLabel(lnCtr));
            if (p_oMaster.getMetaData().getColumnType(lnCtr) == Types.CHAR ||
                p_oMaster.getMetaData().getColumnType(lnCtr) == Types.VARCHAR){
                
                System.out.println("Column index: " + (lnCtr) + " --> Size: " + p_oMaster.getMetaData().getColumnDisplaySize(lnCtr));
            }
        }
        
        System.out.println("----------------------------------------");
        System.out.println("END: MASTER TABLE INFO");
        System.out.println("----------------------------------------");
    }
    
    private void createMaster() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(35);

        meta.setColumnName(1, "sTransNox");
        meta.setColumnLabel(1, "sTransNox");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);

        meta.setColumnName(2, "sBranchCd");
        meta.setColumnLabel(2, "sBranchCd");
        meta.setColumnType(2, Types.VARCHAR);
        meta.setColumnDisplaySize(2, 4);
        
        meta.setColumnName(3, "dTransact");
        meta.setColumnLabel(3, "dTransact");
        meta.setColumnType(3, Types.DATE);

        meta.setColumnName(4, "nCn0001cx");
        meta.setColumnLabel(4, "nCn0001cx");
        meta.setColumnType(4, Types.INTEGER);
        
        meta.setColumnName(5, "nCn0005cx");
        meta.setColumnLabel(5, "nCn0005cx");
        meta.setColumnType(5, Types.INTEGER);
        
        meta.setColumnName(6, "nCn0010cx");
        meta.setColumnLabel(6, "nCn0010cx");
        meta.setColumnType(6, Types.INTEGER);
        
        meta.setColumnName(7, "nCn0025cx");
        meta.setColumnLabel(7, "nCn0025cx");
        meta.setColumnType(7, Types.INTEGER);
        
        meta.setColumnName(8, "nCn0001px");
        meta.setColumnLabel(8, "nCn0001px");
        meta.setColumnType(8, Types.INTEGER);
        
        meta.setColumnName(9, "nCn0005px");
        meta.setColumnLabel(9, "nCn0005px");
        meta.setColumnType(9, Types.INTEGER);
        
        meta.setColumnName(10, "nCn0010px");
        meta.setColumnLabel(10, "nCn0010px");
        meta.setColumnType(10, Types.INTEGER);
        
        meta.setColumnName(11, "nCn0020px");
        meta.setColumnLabel(11, "nCn0020px");
        meta.setColumnType(11, Types.INTEGER);
        
        meta.setColumnName(12, "nNte0020p");
        meta.setColumnLabel(12, "nNte0020p");
        meta.setColumnType(12, Types.INTEGER);
        
        meta.setColumnName(13, "nNte0050p");
        meta.setColumnLabel(13, "nNte0050p");
        meta.setColumnType(13, Types.INTEGER);
        
        meta.setColumnName(14, "nNte0100p");
        meta.setColumnLabel(14, "nNte0100p");
        meta.setColumnType(14, Types.INTEGER);
        
        meta.setColumnName(15, "nNte0200p");
        meta.setColumnLabel(15, "nNte0200p");
        meta.setColumnType(15, Types.INTEGER);
        
        meta.setColumnName(16, "nNte0500p");
        meta.setColumnLabel(16, "nNte0500p");
        meta.setColumnType(16, Types.INTEGER);
        
        meta.setColumnName(17, "nNte1000p");
        meta.setColumnLabel(17, "nNte1000p");
        meta.setColumnType(17, Types.INTEGER);
        
        meta.setColumnName(18, "sORNoxxxx");
        meta.setColumnLabel(18, "sORNoxxxx");
        meta.setColumnType(18, Types.VARCHAR);
        meta.setColumnDisplaySize(18, 10);
        
        meta.setColumnName(19, "sSINoxxxx");
        meta.setColumnLabel(19, "sSINoxxxx");
        meta.setColumnType(19, Types.VARCHAR);
        meta.setColumnDisplaySize(19, 10);
        
        meta.setColumnName(20, "sPRNoxxxx");
        meta.setColumnLabel(20, "sPRNoxxxx");
        meta.setColumnType(20, Types.VARCHAR);
        meta.setColumnDisplaySize(20, 10);
        
        meta.setColumnName(21, "sCRNoxxxx");
        meta.setColumnLabel(21, "sCRNoxxxx");
        meta.setColumnType(21, Types.VARCHAR);
        meta.setColumnDisplaySize(21, 10);
        
        meta.setColumnName(22, "sEntryByx");
        meta.setColumnLabel(22, "sEntryByx");
        meta.setColumnType(22, Types.VARCHAR);
        meta.setColumnDisplaySize(22, 12);
        
        meta.setColumnName(23, "dEntryDte");
        meta.setColumnLabel(23, "dEntryDte");
        meta.setColumnType(23, Types.DATE);
        
        meta.setColumnName(24, "dReceived");
        meta.setColumnLabel(24, "dReceived");
        meta.setColumnType(24, Types.DATE);
        
        meta.setColumnName(25, "sReqstdBy");
        meta.setColumnLabel(25, "sReqstdBy");
        meta.setColumnType(25, Types.VARCHAR);
        meta.setColumnDisplaySize(25, 12);
        
        meta.setColumnName(26, "cReadxxxx");
        meta.setColumnLabel(26, "cReadxxxx");
        meta.setColumnType(26, Types.CHAR);
        meta.setColumnDisplaySize(26, 1);
        
        meta.setColumnName(27, "sReadxxxx");
        meta.setColumnLabel(27, "sReadxxxx");
        meta.setColumnType(27, Types.VARCHAR);
        meta.setColumnDisplaySize(27, 12);
        
        meta.setColumnName(28, "dReadxxxx");
        meta.setColumnLabel(28, "dReadxxxx");
        meta.setColumnType(28, Types.DATE);
        
        meta.setColumnName(29, "cTranStat");
        meta.setColumnLabel(29, "cTranStat");
        meta.setColumnType(29, Types.CHAR);
        meta.setColumnDisplaySize(29, 1);
        
        meta.setColumnName(30, "sModified");
        meta.setColumnLabel(30, "sModified");
        meta.setColumnType(30, Types.VARCHAR);
        meta.setColumnDisplaySize(30, 12);
        
        meta.setColumnName(31, "dModified");
        meta.setColumnLabel(31, "dModified");
        meta.setColumnType(31, Types.DATE);
        
        meta.setColumnName(32, "xBranchNm");
        meta.setColumnLabel(32, "xBranchNm");
        meta.setColumnType(32, Types.VARCHAR);
        
        meta.setColumnName(33, "xEntryByx");
        meta.setColumnLabel(33, "xEntryByx");
        meta.setColumnType(33, Types.VARCHAR);
        
        meta.setColumnName(34, "xReqstdBy");
        meta.setColumnLabel(34, "xReqstdBy");
        meta.setColumnType(34, Types.VARCHAR);
        
        meta.setColumnName(35, "xReadxxxx");
        meta.setColumnLabel(35, "xReadxxxx");
        meta.setColumnType(35, Types.VARCHAR);
        
        p_oMaster = new CachedRowSetImpl();
        p_oMaster.setMetaData(meta);
        
        p_oMaster.last();
        p_oMaster.moveToInsertRow();
        
        MiscUtil.initRowSet(p_oMaster);       
        
        p_oMaster.updateObject("sTransNox", MiscUtil.getNextCode(MASTER_TABLE, "sTransNox", true, p_oApp.getConnection(), p_sBranchCd));
        p_oMaster.updateObject("dTransact", SQLUtil.toDate(p_oApp.getServerDate()));
        p_oMaster.updateObject("cReadxxxx", "0");
        p_oMaster.updateObject("cTranStat", TransactionStatus.STATE_OPEN);
        
        p_oMaster.insertRow();
        p_oMaster.moveToCurrentRow();
    }
    
    private boolean isEntryOK() throws SQLException{           
        //validate master               
        if ((int) getMaster("nCn0001cx") == 0 &&
            (int) getMaster("nCn0005cx") == 0 &&
            (int) getMaster("nCn0025cx") == 0 &&
            (int) getMaster("nCn0001px") == 0 &&
            (int) getMaster("nCn0005px") == 0 &&
            (int) getMaster("nCn0010px") == 0 &&
            (int) getMaster("nNte0020p") == 0 &&
            (int) getMaster("nNte0050p") == 0 &&
            (int) getMaster("nNte0100p") == 0 &&
            (int) getMaster("nNte0200p") == 0 &&
            (int) getMaster("nNte0500p") == 0 &&
            (int) getMaster("nNte1000p") == 0){
            p_sMessage = "No denomination was filled.";
            return false;
        }
        
        return true;
    }
    
    public String getSQ_Master(){
        String lsSQL = "";
        String lsCondition = "";
        String lsStat = String.valueOf(p_nTranStat);
        
        if (lsStat.length() > 1){
            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
                lsSQL += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            
            lsCondition = "a.cTranStat IN (" + lsSQL.substring(2) + ")";
        } else 
            lsCondition = "a.cTranStat = " + SQLUtil.toSQL(lsStat);
                
        lsSQL = "SELECT" + 
                    "  a.sTransNox" +
                    ", a.sBranchCd" +
                    ", a.dTransact" +
                    ", a.nCn0001cx" +
                    ", a.nCn0005cx" +
                    ", a.nCn0010cx" +
                    ", a.nCn0025cx" +
                    ", a.nCn0001px" +
                    ", a.nCn0005px" +
                    ", a.nCn0010px" +
                    ", a.nCn0020px" +
                    ", a.nNte0020p" +
                    ", a.nNte0050p" +
                    ", a.nNte0100p" +
                    ", a.nNte0200p" +
                    ", a.nNte0500p" +
                    ", a.nNte1000p" +
                    ", a.sORNoxxxx" +
                    ", a.sSINoxxxx" +
                    ", a.sPRNoxxxx" +
                    ", a.sCRNoxxxx" +
                    ", a.sEntryByx" +
                    ", a.dEntryDte" +
                    ", a.dReceived" +
                    ", a.sReqstdBy" +
                    ", a.cReadxxxx" +
                    ", a.sReadxxxx" +
                    ", a.dReadxxxx" +
                    ", a.cTranStat" +
                    ", a.sModified" +
                    ", a.dModified" +
                    ", b.sBranchNm xBranchNm" +
                    ", IFNULL(c.sCompnyNm, '') xEntryByx" +
                    ", IFNULL(d.sCompnyNm, '') xReqstdBy" +
                    ", IFNULL(e.sCompnyNm, '') xReadxxxx" +
                " FROM " + MASTER_TABLE + " a" +
                    " LEFT JOIN Branch b ON a.sBranchCd = b.sBranchCd" +
                    " LEFT JOIN Client_Master c ON a.sEntryByx = c.sClientID" +
                    " LEFT JOIN Client_Master d ON a.sReqstdBy = d.sClientID" +
                    " LEFT JOIN Client_Master e ON a.sReadxxxx = e.sClientID";
        
        lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        
        return lsSQL;
    }
    
    private int getColumnIndex(CachedRowSet loRS, String fsValue) throws SQLException{
        int lnIndex = 0;
        int lnRow = loRS.getMetaData().getColumnCount();
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            if (fsValue.equals(loRS.getMetaData().getColumnLabel(lnCtr))){
                lnIndex = lnCtr;
                break;
            }
        }
        
        return lnIndex;
    }
}