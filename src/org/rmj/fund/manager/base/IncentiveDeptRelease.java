package org.rmj.fund.manager.base;


import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
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
import org.rmj.appdriver.MySQLAESCrypt;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.StringUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.constants.UserRight;
import org.rmj.fund.manager.parameters.IncentiveBankInfo;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author User
 */
public class IncentiveDeptRelease {
    private final String FINANCE = "028";
    private final String AUDITOR = "034";
    private final String COLLECTION = "022";
    private final String MAIN_OFFICE = "M001»M0W1";
    
    private final String DEBUG_MODE = "app.debug.mode";
    private final String REQUIRE_CSS = "app.require.css.approval";
    private final String REQUIRE_CM = "app.require.cm.approval";
    private final String REQUIRE_BANK_ON_APPROVAL = "app.require.bank.on.approval";
    
    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    private final IncentiveBankInfo p_oBankInfo;
    
    private String p_sBranchCd;
    private int p_nEditMode;
    private int p_nTranStat;

    private String p_sMessage;
    private boolean p_bWithUI = true;

    private CachedRowSet p_oMaster;
    private CachedRowSet p_oDetail;
    private LMasDetTrans p_oListener;
   
    public IncentiveDeptRelease(GRider foApp, String fsBranchCd, boolean fbWithParent){        
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;        
                
        if (p_sBranchCd.isEmpty()) p_sBranchCd = p_oApp.getBranchCode();
         p_oBankInfo = new IncentiveBankInfo(p_oApp, p_sBranchCd, true);
        p_oBankInfo.setRecordStat(1);
        loadConfig();
        
        p_nTranStat = 0;
        p_nEditMode = EditMode.UNKNOWN;
    }
    
    public void setTranStat(int fnValue){
        p_nTranStat = fnValue;
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
    
    public IncentiveBankInfo getBankInfo(String fsEmployID) throws SQLException{
        if (p_oBankInfo.OpenRecord(fsEmployID))
            return p_oBankInfo;
        else
            return null;
    }
    
    public boolean NewTransaction() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        if (System.getProperty(DEBUG_MODE).equals("0")){
            if (Integer.valueOf(p_oApp.getEmployeeLevel()) < 1){
                p_sMessage = "Your employee level is not authorized to use this transaction.";
                return false;
            }

            if (p_oApp.getUserLevel() < UserRight.SUPERVISOR){
                p_sMessage = "Your account level is not authorized to use this transaction.";
                return false;
            }
        } 
        
        p_sMessage = "";
        
        createMaster();
        createDetail();


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
            if (!p_bWithParent) p_oApp.beginTrans();
            
            //set transaction number on records
            String lsTransNox = MiscUtil.getNextCode("Department_Incentive_Master", "sTransNox", true, p_oApp.getConnection(), p_sBranchCd);
            p_oMaster.updateObject("sTransNox", lsTransNox);
            p_oMaster.updateObject("dTransact", p_oApp.getServerDate());
            p_oMaster.updateObject("dModified", p_oApp.getServerDate());
            p_oMaster.updateObject("sModified", p_oApp.getUserID());
            p_oMaster.updateRow();
            
            lsSQL = MiscUtil.rowset2SQL(p_oMaster, 
                    "Department_Incentive_Master",
                    "xBranchNm;xDeptName;xInctvNme");
            
            if (p_oApp.executeQuery(lsSQL, "Department_Incentive_Master", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                if (!p_bWithParent) p_oApp.rollbackTrans();
                p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                return false;
            }
            
            lnCtr = 1;
            p_oDetail.beforeFirst();
            while (p_oDetail.next()){
                p_oDetail.updateObject("sTransNox", lsTransNox);
                p_oDetail.updateObject("nEntryNox", lnCtr);
                p_oDetail.updateObject("dLastUpdt", p_oApp.getServerDate());
                p_oDetail.updateRow();
                
                lsSQL = MiscUtil.rowset2SQL(p_oDetail, "Department_Incentive_Detail", "xEmployNm;xPositnNm");
                               
                if (p_oApp.executeQuery(lsSQL, "Department_Incentive_Detail", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                    if (!p_bWithParent) p_oApp.rollbackTrans();
                    p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                    return false;
                }
                
                lnCtr++;
            }
            
           
            
            if (!p_bWithParent) p_oApp.commitTrans();
            
            p_nEditMode = EditMode.UNKNOWN;
            return true;
        } else {
            if (!p_bWithParent) p_oApp.beginTrans();
            
            //set transaction number on records
            String lsTransNox = (String) getMaster("sTransNox");
            
            lsSQL = MiscUtil.rowset2SQL(p_oMaster, 
                                        "Department_Incentive_Master",
                                        "xBranchNm;xDeptName;xInctvNme",
                                        "sTransNox = " + SQLUtil.toSQL(lsTransNox));
            
            if (!lsSQL.isEmpty()){
                if (p_oApp.executeQuery(lsSQL, "Department_Incentive_Master", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                    if (!p_bWithParent) p_oApp.rollbackTrans();
                    p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                    return false;
                }
            }
            
            
            lnCtr = 1;
            p_oDetail.beforeFirst();
            while (p_oDetail.next()){
                lsSQL = MiscUtil.rowset2SQL(p_oDetail, 
                                            "Department_Incentive_Detail", 
                                            "xEmployNm;xPositnNm;xSrvcYear", 
                                            "sTransNox = " + SQLUtil.toSQL(lsTransNox) +
                                                " AND nEntryNox = " + p_oDetail.getInt("nEntryNox"));
                
                if (!lsSQL.isEmpty()){
                    if (p_oApp.executeQuery(lsSQL, "Department_Incentive_Detail", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                        if (!p_bWithParent) p_oApp.rollbackTrans();
                        p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                        return false;
                    }
                }
                
                lnCtr++;
            }
            
            
            
            if (!p_bWithParent) p_oApp.commitTrans();
            
            p_nEditMode = EditMode.UNKNOWN;
            return true;
        }
    }
    
    public boolean SearchTransaction(String fsValue, boolean fbByCode) throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        if (System.getProperty(DEBUG_MODE).equals("0")){
            if (Integer.valueOf(p_oApp.getEmployeeLevel()) < 1){
                p_sMessage = "Your employee level is not authorized to use this transaction.";
                return false;
            }

            if (p_oApp.getUserLevel() < UserRight.SUPERVISOR){
                p_sMessage = "Your account level is not authorized to use this transaction.";
                return false;
            }
        }        
        
        String lsSQL = getSQ_Master();
        String lsCondition = "";
        
        if (MAIN_OFFICE.contains(p_oApp.getBranchCode())){            
            if (!(AUDITOR + "»" + COLLECTION + "»" + FINANCE).contains(p_oApp.getDepartment()))
                lsCondition = "a.sDeptIDxx = " + SQLUtil.toSQL(p_oApp.getDepartment());
        } else
            lsCondition = "a.sDeptIDxx LIKE " + SQLUtil.toSQL(p_oApp.getDepartment() + "%");
        
        if (!lsCondition.isEmpty()) lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        
        if (p_bWithUI){
            JSONObject loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "Trans. No.»Department»Date Effective»Remarks", 
                                "sTransNox»xDeptName»dEffctive»sRemarksx", 
                                "a.sTransNox»b.sDeptName»a.dEffctive»a.sRemarksx", 
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
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox LIKE " + SQLUtil.toSQL(fsValue + "%")); 
            lsSQL += " LIMIT 1";
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
        
        if (System.getProperty(DEBUG_MODE).equals("0")){
            if (Integer.valueOf(p_oApp.getEmployeeLevel()) < 1){
                p_sMessage = "Your employee level is not authorized to use this transaction.";
                return false;
            }

            if (p_oApp.getUserLevel() < UserRight.SUPERVISOR){
                p_sMessage = "Your account level is not authorized to use this transaction.";
                return false;
            }
        }  
        
        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        
        //open master
        lsSQL = MiscUtil.addCondition(getSQ_Master(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oMaster = factory.createCachedRowSet();
        p_oMaster.populate(loRS);
        MiscUtil.close(loRS);
        
        //open detail
        lsSQL = MiscUtil.addCondition(getSQ_Detail(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDetail = factory.createCachedRowSet();
        p_oDetail.populate(loRS);
        MiscUtil.close(loRS);
        
       
        
//        computeEmpTotalIncentiveAmount();
        p_nEditMode = EditMode.READY;
        
        return true;
    }
    
    public boolean UpdateTransaction() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        if (System.getProperty(DEBUG_MODE).equals("0")){
            if (Integer.valueOf(p_oApp.getEmployeeLevel()) < 1){
                p_sMessage = "Your employee level is not authorized to use this transaction.";
                return false;
            }

            if (p_oApp.getUserLevel() < UserRight.SUPERVISOR){
                p_sMessage = "Your account level is not authorized to use this transaction.";
                return false;
            }
        } 
        
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid edit mode.";
            return false;
        }
        
        if (p_bWithParent) {
            p_sMessage = "Updating of record from other object is not allowed.";
            return false;
        }
        
        if (Integer.parseInt((String) getMaster("cTranStat")) > 2){
            p_sMessage = "Unable to update processed transactions.";
            return false;
        }
        
        if (MAIN_OFFICE.contains(p_oApp.getBranchCode())){
            if (!p_oApp.getDepartment().equals(AUDITOR)){
                if (!p_oApp.getDepartment().equals((String) getMaster("sDeptIDxx"))){
                    p_sMessage = "Unable to update other department transactions.";
                    return false;
                }
            }
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
        
               
//        //check bank information
//        if (System.getProperty(REQUIRE_BANK_ON_APPROVAL).equals("1")){
//            IncentiveBankInfo loBank;
//            p_oDetail.beforeFirst();
//            while (p_oDetail.next()){
//                loBank = getBankInfo(p_oDetail.getString("sEmployID"));
//
//                if (loBank == null){
//                    p_sMessage = "Some associates has no bank account.";
//                    return false;
//                }
//            }
//        }
        
        String lsTransNox = (String) getMaster("sTransNox");
        String lsSQL = "UPDATE Department_Incentive_Master SET" +
                            " cTranStat = '1'" +
                        " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);
        
        if (p_oApp.executeQuery(lsSQL, "Department_Incentive_Master", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
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
        
        if (!p_oApp.getDepartment().equals(FINANCE)){
            p_sMessage = "Only FM Department can use this feature.";
            return false;
        }
        
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
        String lsSQL = "UPDATE Department_Incentive_Master SET" +
                            "  cTranStat = '2'" +
                        " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);
        
        if (p_oApp.executeQuery(lsSQL, "Department_Incentive_Master", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
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
        
        if (((String) getMaster("cTranStat")).equals("1")){
              p_sMessage = "Transaction was already approved.";
            return false;
           
        }
        
        String lsTransNox = (String) getMaster("sTransNox");
        String lsSQL = "UPDATE Department_Incentive_Master SET" +
                            " cTranStat = '3'" +
                        " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);
        
        if (p_oApp.executeQuery(lsSQL, "Department_Incentive_Master", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public boolean ApprovedTransactionCSS() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid update mode detected.";
            return false;
        }
        
        p_sMessage = "";
        
        if (p_bWithParent) {
            p_sMessage = "Approving transactions from other object is not allowed.";
            return false;
        }
        
        if (!p_oApp.getDepartment().equals(COLLECTION)){
            p_sMessage = "Only CSS Department can use this feature.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("0")){
            p_sMessage = "Unable to approve unconfirmed transactions.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("2")){
            p_sMessage = "Unable to approve posted transactions.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("3")){
            p_sMessage = "Unable to approve cancelled transactions.";
            return false;
        }
        
        if (((String) getMaster("cApprovd1")).equals("1")){
            p_sMessage = "This transaction was already approved by your department.";
            return false;
        }
        
        String lsTransNox = (String) getMaster("sTransNox");
        String lsSQL = "UPDATE Department_Incentive_Master SET" +
                            ", sApproved = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dApproved = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);
        
        if (p_oApp.executeQuery(lsSQL, "Department_Incentive_Master", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public boolean ApprovedTransaction() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid update mode detected.";
            return false;
        }
        
        p_sMessage = "";
        
        if (p_bWithParent) {
            p_sMessage = "Approving transactions from other object is not allowed.";
            return false;
        }
        
        if (!p_oApp.getDepartment().equals(AUDITOR)){
            p_sMessage = "Only CM Department can use this feature.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("0")){
            p_sMessage = "Unable to approve unconfirmed transactions.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("2")){
            p_sMessage = "Unable to approve posted transactions.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("3")){
            p_sMessage = "Unable to approve cancelled transactions.";
            return false;
        }
        
        String lsTransNox = (String) getMaster("sTransNox");
        String lsSQL = "UPDATE Department_Incentive_Master SET" +
                            ", sApproved = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dApproved = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);
        
        if (p_oApp.executeQuery(lsSQL, "Department_Incentive_Master", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    
    
    public int getItemCount() throws SQLException{
        p_oDetail.last();
        return p_oDetail.getRow();
    }
    
    public Object getDetail(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getItemCount() == 0 || fnRow > getItemCount()) return null;
        
        p_oDetail.absolute(fnRow);
        switch (fnIndex){
            case 5://sOldAmtxx
            case 6://sNewAmtxx
                return p_oDetail.getObject(fnIndex);
            default:
                return p_oDetail.getObject(fnIndex);
        }
    }
    
    public Object getDetail(int fnRow, String fsIndex) throws SQLException{
        return getDetail(fnRow, getColumnIndex(p_oDetail, fsIndex));
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
        switch (fnIndex){
            case 3://sDeptIDxx
                searchDepartment((String) foValue, true);
                break;
                
            case 4://sDeptIDxx
                searchIncentive((String) foValue, true);
                break;
//            case 4://sMonthxxx
            case 5://dEffective
                if (foValue instanceof Date){
                    p_oMaster.updateObject(fnIndex, foValue);
                } else
                    p_oMaster.updateObject(fnIndex, foValue);
//                
//                p_oMaster.updateDate(fnIndex, SQLUtil.toDate((Date) foValue));
                p_oMaster.updateRow();
                
                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oMaster.getString(fnIndex));
                break;
            case 6://sRemarksx
            case 12://xBranchNm
            case 13://xDeptName
                p_oMaster.updateString(fnIndex, (String) foValue);
                p_oMaster.updateRow();

                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oMaster.getString(fnIndex));
                break;
        }
    }
    
    public void setMaster(String fsIndex, Object foValue) throws SQLException{
        setMaster(getColumnIndex(p_oMaster, fsIndex), foValue);
    }
    
    public void setDetail(int fnRow, int fnIndex, Object foValue) throws SQLException{
        if (getItemCount()== 0 || fnRow > getItemCount()) return;
        
        p_oDetail.absolute(fnRow);
        switch (fnIndex){
            case 6://sNewAmtxx
                if (StringUtil.isNumeric(String.valueOf(foValue))) 
                    p_oDetail.updateObject(fnIndex, (double) foValue);
                else
                    p_oDetail.updateObject(fnIndex, (0.00));

//                
//                if (p_oListener != null) p_oListener.DetailRetreive(fnRow,fnIndex,p_oDetail.getString(fnIndex));
                break;
            case 7://sRemarksx
            
                p_oDetail.updateString(fnIndex, (String) foValue);

//                if (p_oListener != null) p_oListener.DetailRetreive(fnRow,fnIndex, p_oDetail.getString(fnIndex));
                break;
        }
        
        p_oDetail.updateRow();
        if (p_oListener != null) p_oListener.DetailRetreive(0,0,"");
    }
    
    public void setDetail(int fnRow, String fsIndex, Object foValue) throws SQLException{
        setDetail(fnRow, getColumnIndex(p_oDetail, fsIndex), foValue);
    }
    
    public boolean removeDetail(int fnRow) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW) {
            p_sMessage = "This feature was only for new entries.";
            return false;
        }
        
        if (fnRow > getItemCount()) return false;
        
        p_oDetail.absolute(fnRow);
        p_oDetail.deleteRow();
        
        return true;
    }
//    
//    
//    public boolean searchIncentive(String fsValue, boolean fbByCode) throws SQLException{
//        if (p_nEditMode != EditMode.ADDNEW) {
//            p_sMessage = "This feature was only for new entries.";
//            return false;
//        }
//        
//        int lnCtr;
//        int lnRow = getIncentiveCount();
//        
//        if (fbByCode){
//            for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
//                if (fsValue.equals((String) getIncentiveInfo(lnCtr, "sInctveCD"))) return true;
//            }
//        } else{
//            for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
//                if (fsValue.equals((String) getIncentiveInfo(lnCtr, "xInctvNme"))) return true;
//            }
//        }
//
//        String lsSQL = "SELECT" +
//                            "  sInctveCD" +
//                            ", sInctveDs" +
//                            ", sDivision" +
//                            ", cInctveTp" +
//                            ", cByPercnt" +
//                        " FROM Incentive" +
//                        " WHERE cRecdStat = '1'";
//        
//        JSONObject loJSON;
//        
//        if (p_bWithUI){
//            loJSON = showFXDialog.jsonSearch(
//                        p_oApp, 
//                        lsSQL, 
//                        fsValue, 
//                        "ID»Description", 
//                        "sInctveCD»sInctveDs", 
//                        "sInctveCD»sInctveDs", 
//                        fbByCode ? 0 : 1);
//            
//            if (loJSON != null)
//                return addIncentive((String) loJSON.get("sInctveCD"), (String) loJSON.get("sInctveDs"), (String) loJSON.get("cByPercnt"));
//            else
//                return false;
//        }
//        
//        if (fbByCode)
//            lsSQL = MiscUtil.addCondition(lsSQL, "sInctveCD = " + SQLUtil.toSQL(fsValue));
//        else
//            lsSQL = MiscUtil.addCondition(lsSQL, "sInctveDs LIKE " + SQLUtil.toSQL(fsValue + "%"));
//        
//        lsSQL += " LIMIT 1";
//        ResultSet loRS = p_oApp.executeQuery(lsSQL);
//        
//        JSONArray loArray = MiscUtil.RS2JSON(loRS);
//        MiscUtil.close(loRS);
//        
//        if (loArray.isEmpty()) return false;
//        
//        loJSON = (JSONObject) loArray.get(0);
//        
//        if (loJSON != null)
//            return addIncentive((String) loJSON.get("sInctveCD"), (String) loJSON.get("sInctveDs"), (String) loJSON.get("cByPercnt"));
//        else
//            return false;
//    }
//    
    public boolean searchIncentive(String fsValue, boolean fbByCode) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW) return false;
        
        if (fbByCode)
            if (fsValue.equals((String) getMaster("sInctveCD"))) return true;
        else
            if (fsValue.equals((String) getMaster("xInctvNme"))) return true;
        
            
        String lsSQL = "SELECT" +
                            "  sInctveCD" +
                            ", sInctveDs" +
                        " FROM Incentive" + 
                        " WHERE cRecdStat = '1'";
        
        JSONObject loJSON;
        
        if (p_bWithUI){
            loJSON = showFXDialog.jsonSearch(
                        p_oApp, 
                        lsSQL, 
                        fsValue, 
                        "Code»Description", 
                        "sInctveCD»sInctveDs", 
                        "sInctveCD»sInctveDs", 
                        fbByCode ? 0 : 1);
            
            if (loJSON != null){
                p_oMaster.updateString("sInctveCD", (String) loJSON.get("sInctveCD"));
                p_oMaster.updateString("xInctvNme", (String) loJSON.get("sInctveDs"));
                p_oMaster.updateRow();
                
                //recreate detail and other tables
                createDetail();
               

                if (p_oListener != null) p_oListener.MasterRetreive(13, getMaster("xDeptName"));
                
                return true;
            }
            
            p_oMaster.updateString("sInctveCD", "");
            p_oMaster.updateString("xInctvNme", "");
            p_oMaster.updateRow();
                        
            //recreate detail and other tables
            createDetail();
            
            if (p_oListener != null) p_oListener.MasterRetreive(13, "");
            
            return false;
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sInctveCD = " + SQLUtil.toSQL(fsValue));
        else
            lsSQL = MiscUtil.addCondition(lsSQL, "sInctveDs LIKE " + SQLUtil.toSQL(fsValue + "%"));
        
        lsSQL += " LIMIT 1";
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        JSONArray loArray = MiscUtil.RS2JSON(loRS);
        MiscUtil.close(loRS);
        
        if (loArray.isEmpty()) return false;
        
        loJSON = (JSONObject) loArray.get(0);
        
        p_oMaster.updateString("sInctveCD", (String) loJSON.get("sInctveCD"));
        p_oMaster.updateString("xInctvNme", (String) loJSON.get("sInctveDs"));
        p_oMaster.updateRow();

        if (p_oListener != null) p_oListener.MasterRetreive(14, p_oMaster.getString("sInctveDs"));
        
        //recreate detail and other tables
        createDetail();
        return true;
    }
    
    public boolean searchDepartment(String fsValue, boolean fbByCode) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW) return false;
        
        if (fbByCode)
            if (fsValue.equals((String) getMaster("sDeptIDxx"))) return true;
        else
            if (fsValue.equals((String) getMaster("xDeptName"))) return true;
        
            
        String lsSQL = "SELECT" +
                            "  sDeptIDxx" +
                            ", sDeptName" +
                        " FROM Department" + 
                        " WHERE cRecdStat = '1'";
        
        JSONObject loJSON;
        
        if (p_bWithUI){
            loJSON = showFXDialog.jsonSearch(
                        p_oApp, 
                        lsSQL, 
                        fsValue, 
                        "ID»Department", 
                        "sDeptIDxx»sDeptName", 
                        "sDeptIDxx»sDeptName", 
                        fbByCode ? 0 : 1);
            
            if (loJSON != null){
                p_oMaster.updateString("sDeptIDxx", (String) loJSON.get("sDeptIDxx"));
                p_oMaster.updateString("xDeptName", (String) loJSON.get("sDeptName"));
                p_oMaster.updateRow();
                
                //recreate detail and other tables
                createDetail();
               

                if (p_oListener != null) p_oListener.MasterRetreive(13, getMaster("xDeptName"));
                
                return true;
            }
            
            p_oMaster.updateString("sDeptIDxx", "");
            p_oMaster.updateString("xDeptName", "");
            p_oMaster.updateRow();
                        
            //recreate detail and other tables
            createDetail();
            
            if (p_oListener != null) p_oListener.MasterRetreive(13, "");
            
            return false;
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sDeptIDxx = " + SQLUtil.toSQL(fsValue));
        else
            lsSQL = MiscUtil.addCondition(lsSQL, "sDeptName LIKE " + SQLUtil.toSQL(fsValue + "%"));
        
        lsSQL += " LIMIT 1";
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        JSONArray loArray = MiscUtil.RS2JSON(loRS);
        MiscUtil.close(loRS);
        
        if (loArray.isEmpty()) return false;
        
        loJSON = (JSONObject) loArray.get(0);
        
        p_oMaster.updateString("sDeptIDxx", (String) loJSON.get("sDeptIDxx"));
        p_oMaster.updateString("xDeptName", (String) loJSON.get("sDeptName"));
        p_oMaster.updateRow();

        if (p_oListener != null) p_oListener.MasterRetreive(13, p_oMaster.getString("xDeptName"));
        
        //recreate detail and other tables
        createDetail();
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
    
    public void displayDetFields() throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) return;
        
        int lnRow = p_oDetail.getMetaData().getColumnCount();
        
        System.out.println("----------------------------------------");
        System.out.println("DETAIL TABLE INFO");
        System.out.println("----------------------------------------");
        System.out.println("Total number of columns: " + lnRow);
        System.out.println("----------------------------------------");
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            System.out.println("Column index: " + (lnCtr) + " --> Label: " + p_oDetail.getMetaData().getColumnLabel(lnCtr));
            if (p_oDetail.getMetaData().getColumnType(lnCtr) == Types.CHAR ||
                p_oDetail.getMetaData().getColumnType(lnCtr) == Types.VARCHAR){
                
                System.out.println("Column index: " + (lnCtr) + " --> Size: " + p_oDetail.getMetaData().getColumnDisplaySize(lnCtr));
            }
        }
        
        System.out.println("----------------------------------------");
        System.out.println("END: DETAIL TABLE INFO");
        System.out.println("----------------------------------------");
    }
    
    private void createDetail() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();        

        meta.setColumnCount(9);
        
        meta.setColumnName(1, "sTransNox");
        meta.setColumnLabel(1, "sTransNox");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);
        
        meta.setColumnName(2, "nEntryNox");
        meta.setColumnLabel(2, "nEntryNox");
        meta.setColumnType(2, Types.INTEGER);
        
        meta.setColumnName(3, "sEmployID");
        meta.setColumnLabel(3, "sEmployID");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 12);
        
        meta.setColumnName(4, "dLastUpdt");
        meta.setColumnLabel(4, "dLastUpdt");
        meta.setColumnType(4, Types.DATE);
        
        meta.setColumnName(5, "sOldAmtxx");
        meta.setColumnLabel(5, "sOldAmtxx");
        meta.setColumnType(5, Types.DOUBLE);
        
        meta.setColumnName(6, "sNewAmtxx");
        meta.setColumnLabel(6, "sNewAmtxx");
        meta.setColumnType(6, Types.DOUBLE);
        
        meta.setColumnName(7, "sRemarksx");
        meta.setColumnLabel(7, "sRemarksx");
        meta.setColumnType(7, Types.VARCHAR);
        meta.setColumnDisplaySize(7, 128);
        
        meta.setColumnName(8, "xEmployNm");
        meta.setColumnLabel(8, "xEmployNm");
        meta.setColumnType(8, Types.VARCHAR);
        
        meta.setColumnName(9, "xPositnNm");
        meta.setColumnLabel(9, "xPositnNm");
        meta.setColumnType(9, Types.VARCHAR);
        
        p_oDetail = new CachedRowSetImpl();
        p_oDetail.setMetaData(meta);        
        
        String lsSQL = "SELECT " +
        "      a.sEmployID " +
        "    , IFNULL(b.sCompnyNm, '') xEmployNm " +
        "    , IFNULL(d.sPositnNm, '') xPositnNm " +
//        "    , IFNULL(a.sEmpLevID, '') xEmpLevID " +
        "    , IFNULL(a.sDeptIDxx, '') sDeptIDxx " +
        "    , IFNULL(f.sBankAcct, '') xBankAcct " +
//        "    , IFNULL(ROUND(DATEDIFF(NOW(), IFNULL(a.dStartEmp, a.dHiredxxx)) / 365), '') xSrvcYear " +
        " FROM Employee_Master001 a " +
        "     LEFT JOIN Client_Master b ON a.sEmployID = b.sClientID " +
//        "     LEFT JOIN Employee_Level c ON a.sEmpLevID = c.sEmpLevID " +
        "     LEFT JOIN `Position` d ON a.sPositnID = d.sPositnID " +
              " LEFT JOIN `Employee_Incentive_Bank_Info` f ON a.sEmployID = f.sEmployID" +
        " WHERE a.sBranchCd = " + SQLUtil.toSQL(p_sBranchCd) +
        "     AND a.cRecdStat = '1' " +
        "     AND ISNULL(a.dFiredxxx) " +
        " ORDER BY xEmployNm";   
//      
//        String lsSQL2 =   " UNION SELECT" +
//        "      h.sEmployID " +
//        "    , IFNULL(i.sCompnyNm, '') xEmployNm " +
//        "    , IFNULL(k.sPositnNm, '') xPositnNm " +
//        "    , IFNULL(h.sDeptIDxx, '') sDeptIDxx " +
//        "     FROM  Branch e" +
//        "	 ,Branch_Others f " +
//        "	 , Branch_Area g " +
//        "     LEFT JOIN Employee_Master001 h " +
//        "	ON g.sAreaMngr = h.sEmployID   " +
//        "     LEFT JOIN Client_Master i ON h.sEmployID = i.sClientID " +
//        "     LEFT JOIN `Position` k ON h.sPositnID = k.sPositnID" +
//        "	WHERE e.sBranchCd = f.sBranchCd" +
//        "	AND  f.sAreaCode = g.sAreaCode" +
//        "	AND e.sBranchCd = " + SQLUtil.toSQL(p_sBranchCd) +
//        " ORDER BY xEmployNm";   
        p_oMaster.first();
        if (!p_oMaster.getString("sDeptIDxx").isEmpty())
            lsSQL = MiscUtil.addCondition(lsSQL, "sDeptIDxx = " + SQLUtil.toSQL(p_oMaster.getString("sDeptIDxx")));
//            lsSQL = lsSQL + lsSQL2;
//            lsSQL = MiscUtil.addCondition(lsSQL, "sDeptIDxx = " + SQLUtil.toSQL(p_oMaster.getString("sDeptIDxx")));
//            
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        int lnRow = 1;
        while (loRS.next()){
            p_oDetail.last();
            p_oDetail.moveToInsertRow();

            MiscUtil.initRowSet(p_oDetail);        
            p_oDetail.updateInt("nEntryNox", lnRow);
            p_oDetail.updateString("sEmployID", loRS.getString("sEmployID"));
            p_oDetail.updateString("xEmployNm", loRS.getString("xEmployNm"));
//            p_oDetail.updateString("xEmpLevNm", loRS.getString("xEmpLevNm"));
            p_oDetail.updateString("xPositnNm", loRS.getString("xPositnNm"));
            p_oDetail.updateObject("dLastUpdt", p_oApp.getServerDate());
            p_oDetail.updateObject("sOldAmtxx", 0.00);
            p_oDetail.updateObject("sNewAmtxx", 0.00);
            
            
            p_oDetail.insertRow();
            p_oDetail.moveToCurrentRow();
            lnRow++;
        }
       
    }
    
    private void createMaster() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(14);

        meta.setColumnName(1, "sTransNox");
        meta.setColumnLabel(1, "sTransNox");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);

        meta.setColumnName(2, "dTransact");
        meta.setColumnLabel(2, "dTransact");
        meta.setColumnType(2, Types.DATE);

        meta.setColumnName(3, "sDeptIDxx");
        meta.setColumnLabel(3, "sDeptIDxx");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 4);

        meta.setColumnName(4, "sInctveCD");
        meta.setColumnLabel(4, "sInctveCD");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 4);
        
        meta.setColumnName(5, "dEffctive");
        meta.setColumnLabel(5, "dEffctive");
        meta.setColumnType(5, Types.DATE);
        
        meta.setColumnName(6, "sRemarksx");
        meta.setColumnLabel(6, "sRemarksx");
        meta.setColumnDisplaySize(5, 128);
        
        meta.setColumnName(7, "cTranStat");
        meta.setColumnLabel(7, "cTranStat");
        meta.setColumnType(7, Types.CHAR);
        meta.setColumnDisplaySize(8, 1);
        
        meta.setColumnName(8, "sApproved");
        meta.setColumnLabel(8, "sApproved");
        meta.setColumnType(8, Types.VARCHAR);
        meta.setColumnDisplaySize(8, 10);
        
        meta.setColumnName(9, "dApproved");
        meta.setColumnLabel(9, "dApproved");
        meta.setColumnType(9, Types.DATE);
        
        meta.setColumnName(10, "sModified");
        meta.setColumnLabel(10, "sModified");
        meta.setColumnType(10, Types.VARCHAR);
        meta.setColumnDisplaySize(10, 10);
        
        meta.setColumnName(11, "dModified");
        meta.setColumnLabel(11, "dModified");
        meta.setColumnType(11, Types.VARCHAR);
        meta.setColumnDisplaySize(12, 10);
        
        meta.setColumnName(12, "xBranchNm");
        meta.setColumnLabel(12, "xBranchNm");
        meta.setColumnType(12, Types.VARCHAR);
        
        meta.setColumnName(13, "xDeptName");
        meta.setColumnLabel(13, "xDeptName");
        meta.setColumnType(13, Types.VARCHAR);
        
        meta.setColumnName(14, "xInctvNme");
        meta.setColumnLabel(14, "xInctvNme");
        meta.setColumnType(14, Types.VARCHAR);
        
        p_oMaster = new CachedRowSetImpl();
        p_oMaster.setMetaData(meta);
        
        p_oMaster.last();
        p_oMaster.moveToInsertRow();
        
        MiscUtil.initRowSet(p_oMaster);       
        
        p_oMaster.updateObject("sTransNox", MiscUtil.getNextCode("Department_Incentive_Master", "sTransNox", true, p_oApp.getConnection(), p_sBranchCd));
        p_oMaster.updateObject("dTransact", p_oApp.getServerDate());
        p_oMaster.updateObject("dEffctive", p_oApp.getServerDate());
        p_oMaster.updateObject("cTranStat", TransactionStatus.STATE_OPEN);
        p_oMaster.updateObject("xBranchNm", p_oApp.getBranchName());
        System.out.println(p_oApp.getServerDate());
        p_oMaster.insertRow();
        p_oMaster.moveToCurrentRow();
    }
    
    private double DecryptAmount(String fsValue){
        return Double.valueOf(MySQLAESCrypt.Decrypt(fsValue, p_oApp.SIGNATURE));
    }
    
    private String EncryptAmount(double fnValue){
        return MySQLAESCrypt.Encrypt(String.valueOf(fnValue), p_oApp.SIGNATURE);
    }
    
    private boolean isEntryOK() throws SQLException{    
        if (System.getProperty(DEBUG_MODE).equals("0")){
            if (Integer.valueOf(p_oApp.getEmployeeLevel()) < 1){
                p_sMessage = "Your employee level is not authorized to use this transaction.";
                return false;
            }

            if (p_oApp.getUserLevel() < UserRight.SUPERVISOR){
                p_sMessage = "Your account level is not authorized to use this transaction.";
                return false;
            }
        }  
        
        //validate master
        p_oMaster.first();
        if (p_oMaster.getString("dEffctive").isEmpty()){
            p_sMessage = "Effective period must not be empty.";
            return false;
        }
        if (p_oMaster.getString("sDeptIDxx").isEmpty()){
            p_sMessage = "Department must not be empty.";
            return false;
        }
        if (p_oMaster.getString("sInctveCD").isEmpty()){
            p_sMessage = "Incentive must not be empty.";
            return false;
        }
        
        //validate detail
        if (getItemCount() == 0){
            p_sMessage = "No employee detected.";
            return false;
        }
        
        p_oDetail.beforeFirst();
        while (p_oDetail.next()){            
//            if (DecryptAmount(p_oDetail.getString("sNewAmtxx")) < 0.00){
//                p_sMessage = p_oDetail.getString("sNewAmtxx") + " has negative new incentive amount.";
//                return false;
//            }  
            if ((p_oDetail.getDouble("sNewAmtxx")) < 0.00){
                p_sMessage = p_oDetail.getString("sNewAmtxx") + " has negative new incentive amount.";
                return false;
            }  
        }
        
        return true;
    }
    
    public String getSQ_Master(){
        String lsSQL = "";
        String lsStat = String.valueOf(p_nTranStat);
        
        if (lsStat.length() > 1){
            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
                lsSQL += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            
            lsSQL = " AND a.cTranStat IN (" + lsSQL.substring(2) + ")";
        } else{            
            lsSQL = " AND a.cTranStat = " + SQLUtil.toSQL(lsStat);
        }
                
        lsSQL = "SELECT" + 
                    "  IFNULL(a.sTransNox,'') sTransNox" +
                    ", IFNULL(a.dTransact,'') dTransact" +
                    ", IFNULL(a.sDeptIDxx,'') sDeptIDxx" +
                    ", IFNULL(a.sInctveCD,'') sInctveCD" +
                    ", IFNULL(a.sRemarksx,'') sRemarksx" +
                    ", IFNULL(a.dEffctive,'') dEffctive" +
                    ", IFNULL(a.cTranStat,'') cTranStat" +
                    ", IFNULL(a.sApproved,'') sApproved" +
                    ", IFNULL(a.dApproved,'') dApproved" +
                    ", IFNULL(a.sModified,'') sModified" +
                    ", IFNULL(a.dModified,'') dModified" +
                    ", IFNULL(c.sBranchNm,'') xBranchNm" +
                    ", IFNULL(b.sDeptName, '') xDeptName" +
                    ", IFNULL(d.sInctveDs, '') xInctvNme" +
                " FROM Department_Incentive_Master a" +
                        " LEFT JOIN Department b ON a.sDeptIDxx = b.sDeptIDxx" +
                        " LEFT JOIN Incentive d ON a.sInctveCD = d.sInctveCD" +
                    ", Branch c " +
                " WHERE LEFT(a.sTransNox, 4) = c.sBranchCd" +
                    lsSQL;
        
        return lsSQL;
    }
    
    private String getSQ_Detail(){
        return "SELECT" +
                    "  IFNULL(a.sTransNox,'') sTransNox" +
                    ", IFNULL(a.nEntryNox,'') nEntryNox" +
                    ", IFNULL(a.sEmployID,'') sEmployID" +
                    ", IFNULL(a.dLastUpdt,'') dLastUpdt" +
                    ", IFNULL(a.sOldAmtxx,0) sOldAmtxx" +
                    ", IFNULL(a.sNewAmtxx,0) sNewAmtxx" +
                    ", IFNULL(a.sRemarksx,'') sRemarksx" +
                    ", IFNULL(c.sCompnyNm,'') xEmployNm " +
                    ", IFNULL(e.sPositnNm, '') xPositnNm " +
                    ", IFNULL(f.sBankAcct, '') xBankAcct " +
                " FROM Department_Incentive_Detail a" +
              " LEFT JOIN `Employee_Incentive_Bank_Info` f ON a.sEmployID = f.sEmployID" +
                    ", Employee_Master001 b " +
                        " LEFT JOIN Client_Master c ON b.sEmployID = c.sClientID" +
//                        " LEFT JOIN Employee_Level d ON b.sEmpLevID = d.sEmpLevID" +
                        " LEFT JOIN `Position` e ON b.sPositnID = e.sPositnID" +
                " WHERE a.sEmployID = b.sEmployID " +
                " ORDER BY nEntryNox ";
    }
    
    
    private int getColumnIndex(CachedRowSet loRS, String fsValue) throws SQLException{
       
        int lnIndex = 0;
        if(loRS != null){
            int lnRow = loRS.getMetaData().getColumnCount();
        
            for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                if (fsValue.equals(loRS.getMetaData().getColumnLabel(lnCtr))){
                    lnIndex = lnCtr;
                    break;
                }
            }
        }
        
       
        return lnIndex;
    }
    
    private void loadConfig(){
        //update the value on configuration before deployment
        System.setProperty(DEBUG_MODE, "0"); 
        System.setProperty(REQUIRE_CSS, "0");
        System.setProperty(REQUIRE_CM, "1");
        System.setProperty(REQUIRE_BANK_ON_APPROVAL, "0");
    }
}
