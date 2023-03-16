package org.rmj.fund.manager.base;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.Types;
import java.sql.SQLException;
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

/**
 * @author Michael Cuison
 * @since October 11, 2021
 */
public class Incentive {
    private final String FINANCE = "028";
    private final String AUDITOR = "034";
    private final String COLLECTION = "022";
    private final String MIS = "026";
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
    private CachedRowSet p_oAllctn;
    private CachedRowSet p_oAllctn_Emp;
    private CachedRowSet p_oDedctn;
    private CachedRowSet p_oDedctn_Emp;
    
    private LMasDetTrans p_oListener;
   
    public Incentive(GRider foApp, String fsBranchCd, boolean fbWithParent){        
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
        
        if (p_oApp.getUserLevel() < UserRight.AUDIT){
            p_sMessage = "Your account level is not authorized to use this transaction.";
            return false;
        }
        p_sMessage = "";
        
        createMaster();
        createDetail();

        createDetailAllocation();
        createDetailAllocationEmp();
        createDetailDeductionAlloc();
        createDetailDeductionAllocEmp();

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
            String lsTransNox = MiscUtil.getNextCode("Incentive_Master", "sTransNox", true, p_oApp.getConnection(), p_sBranchCd);
            p_oMaster.updateObject("sTransNox", lsTransNox);
            p_oMaster.updateObject("dTransact", p_oApp.getServerDate());
            p_oMaster.updateObject("dPrepared", p_oApp.getServerDate());
            p_oMaster.updateObject("sPrepared", p_oApp.getUserID());
            p_oMaster.updateRow();
            
            lsSQL = MiscUtil.rowset2SQL(p_oMaster, "Incentive_Master", "xBranchNm;xDeptName");
            
            if (p_oApp.executeQuery(lsSQL, "Incentive_Master", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                if (!p_bWithParent) p_oApp.rollbackTrans();
                p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                return false;
            }
            
            lnCtr = 1;
            p_oDetail.beforeFirst();
            while (p_oDetail.next()){
                p_oDetail.updateObject("sTransNox", lsTransNox);
                p_oDetail.updateObject("nEntryNox", lnCtr);
                p_oDetail.updateRow();
                
                lsSQL = MiscUtil.rowset2SQL(p_oDetail, "Incentive_Detail", "xEmployNm;xEmpLevNm;xPositnNm;xSrvcYear;xIncentve;xDeductnx");
                               
                if (p_oApp.executeQuery(lsSQL, "Incentive_Detail", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                    if (!p_bWithParent) p_oApp.rollbackTrans();
                    p_sMessage = p_oApp.getMessage() + " ; " + p_oApp.getErrMsg();
                    return false;
                }
                
                lnCtr++;
            }
            
            p_oAllctn.beforeFirst();
            while (p_oAllctn.next()){
                p_oAllctn.updateObject("sTransNox", lsTransNox);
                p_oAllctn.updateRow();
                
                lsSQL = MiscUtil.rowset2SQL(p_oAllctn, "Incentive_Detail_Allocation", "xInctvNme;xByPercnt");
                
                if (p_oApp.executeQuery(lsSQL, "Incentive_Detail_Allocation", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                    if (!p_bWithParent) p_oApp.rollbackTrans();
                    p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                    return false;
                }
            }
            
            p_oAllctn_Emp.beforeFirst();
            while (p_oAllctn_Emp.next()){
                p_oAllctn_Emp.updateObject("sTransNox", lsTransNox);
                p_oAllctn_Emp.updateRow();
                
                lsSQL = MiscUtil.rowset2SQL(p_oAllctn_Emp, "Incentive_Detail_Allocation_Employee", "xEmployNm;xInctvNme;nTotalAmt;nNewValue");
                
                if (p_oApp.executeQuery(lsSQL, "Incentive_Detail_Allocation_Employee", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                    if (!p_bWithParent) p_oApp.rollbackTrans();
                    p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                    return false;
                }
            }
            
            if (getDeductionCount() > 0){
                p_oDedctn.beforeFirst();
                while (p_oDedctn.next()){
                    p_oDedctn.updateObject("sTransNox", lsTransNox);
                    p_oDedctn.updateRow();

                    lsSQL = MiscUtil.rowset2SQL(p_oDedctn, "Incentive_Detail_Ded_Allocation", "xNewValue");
                    
                    if (p_oApp.executeQuery(lsSQL, "Incentive_Detail_Ded_Allocation", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                        if (!p_bWithParent) p_oApp.rollbackTrans();
                        p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                        return false;
                    }
                }
                
                p_oDedctn_Emp.beforeFirst();
                while (p_oDedctn_Emp.next()){
                    p_oDedctn_Emp.updateObject("sTransNox", lsTransNox);
                    p_oDedctn_Emp.updateRow();

                    lsSQL = MiscUtil.rowset2SQL(p_oDedctn_Emp, "Incentive_Detail_Ded_Allocation_Employee", "xEmployNm;nTotalAmt;nNewValue");
                    
                    if (p_oApp.executeQuery(lsSQL, "Incentive_Detail_Ded_Allocation_Employee", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                        if (!p_bWithParent) p_oApp.rollbackTrans();
                        p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                        return false;
                    }
                }
            }
            
            if (!p_bWithParent) p_oApp.commitTrans();
            
            p_nEditMode = EditMode.UNKNOWN;
            return true;
        } else {
            if (!p_bWithParent) p_oApp.beginTrans();
            
            //set transaction number on records
            String lsTransNox = (String) getMaster("sTransNox");
            
            lsSQL = MiscUtil.rowset2SQL(p_oMaster, 
                                        "Incentive_Master", 
                                        "xBranchNm;xDeptName", 
                                        "sTransNox = " + SQLUtil.toSQL(lsTransNox));
            
            if (!lsSQL.isEmpty()){
                if (p_oApp.executeQuery(lsSQL, "Incentive_Master", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                    if (!p_bWithParent) p_oApp.rollbackTrans();
                    p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                    return false;
                }
            }
            
            
            lnCtr = 1;
            p_oDetail.beforeFirst();
            while (p_oDetail.next()){
                lsSQL = MiscUtil.rowset2SQL(p_oDetail, 
                                            "Incentive_Detail", 
                                            "xEmployNm;xEmpLevNm;xPositnNm;xSrvcYear;xIncentve;xDeductnx", 
                                            "sTransNox = " + SQLUtil.toSQL(lsTransNox) +
                                                " AND nEntryNox = " + p_oDetail.getInt("nEntryNox"));
                
                if (!lsSQL.isEmpty()){
                    if (p_oApp.executeQuery(lsSQL, "Incentive_Detail", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                        if (!p_bWithParent) p_oApp.rollbackTrans();
                        p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                        return false;
                    }
                }
                
                lnCtr++;
            }
            
            p_oAllctn.beforeFirst();
            while (p_oAllctn.next()){                
                lsSQL = MiscUtil.rowset2SQL(p_oAllctn, 
                                            "Incentive_Detail_Allocation", 
                                            "xInctvNme;xByPercnt",
                                            "sTransNox = " + SQLUtil.toSQL(lsTransNox) +
                                                " AND sInctveCD = " + SQLUtil.toSQL(p_oAllctn.getString("sInctveCD")));
                
                if (!lsSQL.isEmpty()){
                    if (p_oApp.executeQuery(lsSQL, "Incentive_Detail_Allocation", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                        if (!p_bWithParent) p_oApp.rollbackTrans();
                        p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                        return false;
                    }
                }                
            }
            
            p_oAllctn_Emp.beforeFirst();
            while (p_oAllctn_Emp.next()){
                lsSQL = MiscUtil.rowset2SQL(p_oAllctn_Emp, 
                                            "Incentive_Detail_Allocation_Employee", 
                                            "xEmployNm;xInctvNme;nTotalAmt",
                                            "sTransNox = " + SQLUtil.toSQL(lsTransNox) +
                                                " AND sInctveCD = " + SQLUtil.toSQL(p_oAllctn_Emp.getString("sInctveCD")) +
                                                " AND sEmployID = " + SQLUtil.toSQL(p_oAllctn_Emp.getString("sEmployID")));
                
                if (!lsSQL.isEmpty()){
                    if (p_oApp.executeQuery(lsSQL, "Incentive_Detail_Allocation_Employee", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                        if (!p_bWithParent) p_oApp.rollbackTrans();
                        p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                        return false;
                    }
                }
            }
            
            if (getDeductionCount() > 0){
                p_oDedctn.beforeFirst();
                while (p_oDedctn.next()){
                    if(p_oDedctn.getString("xNewValue").equalsIgnoreCase("0")){
                        p_oDedctn.updateObject("sTransNox", lsTransNox);
                        p_oDedctn.updateRow();

                        lsSQL = MiscUtil.rowset2SQL(p_oDedctn, "Incentive_Detail_Ded_Allocation", "xNewValue");

                        if (p_oApp.executeQuery(lsSQL, "Incentive_Detail_Ded_Allocation", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                            if (!p_bWithParent) p_oApp.rollbackTrans();
                            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                            return false;
                        }
                        
                    }else{
                        lsSQL = MiscUtil.rowset2SQL(p_oDedctn, 
                                                "Incentive_Detail_Ded_Allocation", 
                                                "xNewValue",
                                                "sTransNox = " + SQLUtil.toSQL(lsTransNox) +
                                                    " AND nEntryNox = " + p_oDedctn.getInt("nEntryNox"));
                    
                        if (!lsSQL.isEmpty()){
                            if (p_oApp.executeQuery(lsSQL, "Incentive_Detail_Ded_Allocation", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                                if (!p_bWithParent) p_oApp.rollbackTrans();
                                p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                                return false;
                            }
                        }
                        
                    }
                    
                }
                
                p_oDedctn_Emp.beforeFirst();
                while (p_oDedctn_Emp.next()){
                    if(p_oDedctn_Emp.getString("nNewValue").equalsIgnoreCase("0")){
                        p_oDedctn_Emp.updateObject("sTransNox", lsTransNox);
                        p_oDedctn_Emp.updateRow();

                        lsSQL = MiscUtil.rowset2SQL(p_oDedctn_Emp, "Incentive_Detail_Ded_Allocation_Employee", "xEmployNm;nTotalAmt;nNewValue");

                        if (p_oApp.executeQuery(lsSQL, "Incentive_Detail_Ded_Allocation_Employee", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                            if (!p_bWithParent) p_oApp.rollbackTrans();
                            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                            return false;
                        }
                        
                    }else{
                        lsSQL = MiscUtil.rowset2SQL(p_oDedctn_Emp, 
                                                    "Incentive_Detail_Ded_Allocation_Employee", 
                                                    "xEmployNm;nTotalAmt;nNewValue",
                                                    "sTransNox = " + SQLUtil.toSQL(lsTransNox) +
                                                        " AND nEntryNox = " + p_oDedctn_Emp.getInt("nEntryNox") +
                                                        " AND sEmployID = " + SQLUtil.toSQL(p_oDedctn_Emp.getString("sEmployID")));

                        if (!lsSQL.isEmpty()){
                            if (p_oApp.executeQuery(lsSQL, "Incentive_Detail_Ded_Allocation_Employee", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
                                if (!p_bWithParent) p_oApp.rollbackTrans();
                                p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                                return false;
                            }
                        }  
                    }
                }
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
            if (!(AUDITOR + "»" + COLLECTION + "»" + FINANCE).contains(p_oApp.getDepartment())){
                if (!p_oApp.getDepartment().equals(AUDITOR)) lsCondition = "a.sDeptIDxx = " + SQLUtil.toSQL(p_oApp.getDepartment());
            }
        } else{
            if (!p_oApp.isMainOffice()) lsCondition =  "  LEFT(a.sTransNox, 4) = " +SQLUtil.toSQL(p_oApp.getBranchCode());
        }
        if (!lsCondition.isEmpty()) lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox LIKE " + SQLUtil.toSQL(fsValue + "%"));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "c.sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%")); 
        }
        
        System.out.println(lsSQL);
        if (p_bWithUI){
            JSONObject loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "Trans. No.»Branch»Period»Remarks", 
                                "a.sTransNox»xBranchNm»a.sMonthxxx»a.sRemarksx", 
                                "a.sTransNox»c.sBranchNm»a.sMonthxxx»a.sRemarksx", 
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
        
        //open incentive
        lsSQL = MiscUtil.addCondition(getSQ_Detail_Allocation(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oAllctn = factory.createCachedRowSet();
        p_oAllctn.populate(loRS);
        MiscUtil.close(loRS);
        
        //open incentive employee allocation
        lsSQL = MiscUtil.addCondition(getSQ_Detail_Allocation_Emp(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oAllctn_Emp = factory.createCachedRowSet();
        p_oAllctn_Emp.populate(loRS);
        MiscUtil.close(loRS);
        
        //open deductions
        lsSQL = MiscUtil.addCondition(getSQ_Detail_Deduction(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDedctn = factory.createCachedRowSet();
        p_oDedctn.populate(loRS);
        MiscUtil.close(loRS);
        
        //open deductions employee alloction
        lsSQL = MiscUtil.addCondition(getSQ_Detail_Deduction_Emp(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDedctn_Emp = factory.createCachedRowSet();
        p_oDedctn_Emp.populate(loRS);
        MiscUtil.close(loRS);
        
        computeEmpTotalIncentiveAmount();
        p_nEditMode = EditMode.READY;
        
        return true;
    }
    public boolean SearchEmployee(String fsValue, boolean fbByCode) throws SQLException{
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
        
        String lsSQL = getSQ_Employee();
        String lsCondition = "";
        
//        if (MAIN_OFFICE.contains(p_oApp.getBranchCode())){            
//            if (!(AUDITOR + "»" + COLLECTION + "»" + FINANCE).contains(p_oApp.getDepartment()))
//                lsCondition = "a.sDeptIDxx = " + SQLUtil.toSQL(p_oApp.getDepartment());
//        } else
//            lsCondition = "b.sCompnyNm LIKE " + SQLUtil.toSQL(fsValue + "%");
        lsCondition = "b.sCompnyNm LIKE " + SQLUtil.toSQL(fsValue + "%");
        
        if (!lsCondition.isEmpty()) lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        
        if (p_bWithUI){
            JSONObject loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "Employee No.»Name»Level»Position", 
                                "sEmployID»xEmployNm»xEmpLevNm»xPositnNm", 
                                "a.sEmployID»b.sCompnyNm»c.sEmpLevNm»d.sPositnNm", 
                                fbByCode ? 0 : 1);
            
            if (loJSON != null){
                int lnRow = 1;
                for(int lnCtr = 1; lnCtr<=getItemCount(); lnCtr++){
                    lnRow++;
                }
                
                p_oDetail.last();
                p_oDetail.moveToInsertRow();
                MiscUtil.initRowSet(p_oDetail);        
                p_oDetail.updateInt("nEntryNox", lnRow);
                p_oDetail.updateString("sEmployID", (String) loJSON.get("sEmployID"));
                p_oDetail.updateString("xEmployNm", (String) loJSON.get("xEmployNm"));
                p_oDetail.updateString("xEmpLevNm", (String) loJSON.get("xEmpLevNm"));
                p_oDetail.updateString("xPositnNm", (String) loJSON.get("xPositnNm"));
                p_oDetail.updateString("xSrvcYear", (String) loJSON.get("xSrvcYear"));
                p_oDetail.updateString("nTotalAmt", EncryptAmount(0.00));
                p_oDetail.updateDouble("xIncentve", 0.00);
                p_oDetail.updateDouble("xDeductnx", 0.00);


                p_oDetail.insertRow();
                p_oDetail.moveToCurrentRow();
                return true;
            }
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "b.sCompnyNm = " + SQLUtil.toSQL(fsValue));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "b.sCompnyNm LIKE " + SQLUtil.toSQL(fsValue + "%")); 
            lsSQL += " LIMIT 1";
        }
        
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        if (!loRS.next()){
            MiscUtil.close(loRS);
            p_sMessage = "No transaction found for the givern criteria.";
            return false;
        }
        
        lsSQL = loRS.getString("xEmployNm");
        MiscUtil.close(loRS);
        
        return OpenTransaction(lsSQL);
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
        
//        if (MAIN_OFFICE.contains(p_oApp.getBranchCode())){
//            if (!p_oApp.getDepartment().equals(AUDITOR)){
//                System.out.println(p_oApp.getDepartment());
//                System.out.println("Master Dept = " + getMaster("sDeptIDxx"));
//                if (!p_oApp.getDepartment().equals((String) getMaster("sDeptIDxx"))){
//                    p_sMessage = "Unable to update other department transactions.";
//                    return false;
//                }
//            }
//        }
        
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
            if (System.getProperty(REQUIRE_CSS).equals("1")){
                if (((String) getMaster("cApprovd1")).equals("0")){
                    return ApprovedTransactionCSS();
                }
            }
            
            if (System.getProperty(REQUIRE_CM).equals("1")){
                if (((String) getMaster("cApprovd2")).equals("0")){
                    return ApprovedTransactionCM();
                }
            }
            
            p_sMessage = "Transaction was already confirmed.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("2")){
            p_sMessage = "Unable to confirm posted transactions.";
            return false;
        }
        
        if (MAIN_OFFICE.contains(p_oApp.getBranchCode())){
            if (!p_oApp.getDepartment().equals(AUDITOR)){
                if (!p_oApp.getDepartment().equals((String) getMaster("sDeptIDxx"))){
                    p_sMessage = "Unable to confirm other department transactions.";
                    return false;
                }
            }
        }
               
        //check bank information
        if (System.getProperty(REQUIRE_BANK_ON_APPROVAL).equals("1")){
            IncentiveBankInfo loBank;
            p_oDetail.beforeFirst();
            while (p_oDetail.next()){
                loBank = getBankInfo(p_oDetail.getString("sEmployID"));

                if (loBank == null){
                    p_sMessage = "Some associates has no bank account.";
                    return false;
                }
            }
        }
        
        String lsTransNox = (String) getMaster("sTransNox");
        String lsSQL = "UPDATE Incentive_Master SET" +
                            " cTranStat = '1'" +
                        " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);
        
        if (p_oApp.executeQuery(lsSQL, "Incentive_Master", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
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
        
        if (System.getProperty(REQUIRE_CSS).equals("1")){
            if (((String) getMaster("cApprovd1")).equals("0")){
                p_sMessage = "This transaction was not yet approved by CSS Department.";
                return false;
            }
        }
        if (System.getProperty(REQUIRE_CM).equals("1")){
            if (((String) getMaster("cApprovd2")).equals("1")){
                p_sMessage = "This transaction was not yet approved by CM Department.";
                return false;
            }
        }
        
        String lsTransNox = (String) getMaster("sTransNox");
        String lsSQL = "UPDATE Incentive_Master SET" +
                            "  cTranStat = '2'" +
                        " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);
        
        if (p_oApp.executeQuery(lsSQL, "Incentive_Master", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
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
            if (!(MAIN_OFFICE.contains(p_oApp.getBranchCode()) &&
                p_oApp.getDepartment().equals(AUDITOR))){
                p_sMessage = "Only CM Department can cancel confirmed transactions.";
                return false;
            } else {
                if ("1".equals((String) getMaster("cApprovd2"))){
                    p_sMessage = "This transaction was already CM Confirmed. Unable to disapprove.";
                    return false;
                }
            }
        }
        
        String lsTransNox = (String) getMaster("sTransNox");
        String lsSQL = "UPDATE Incentive_Master SET" +
                            " cTranStat = '3'" +
                        " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);
        
        if (p_oApp.executeQuery(lsSQL, "Incentive_Master", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
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
        String lsSQL = "UPDATE Incentive_Master SET" +
                            "  cApprovd1 = '1'" +
                            ", sApprovd1 = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dApprovd1 = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);
        
        if (p_oApp.executeQuery(lsSQL, "Incentive_Master", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public boolean ApprovedTransactionCM() throws SQLException{
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
        
        if (System.getProperty(REQUIRE_CSS).equals("1")){
            if (((String) getMaster("cApprovd1")).equals("0")){
                p_sMessage = "This transaction was not yet approved by CSS Department.";
                return false;
            }
        }
        
        if (((String) getMaster("cApprovd2")).equals("1")){
            p_sMessage = "This transaction was already approved by your Department.";
            return false;
        }
        
        String lsTransNox = (String) getMaster("sTransNox");
        String lsSQL = "UPDATE Incentive_Master SET" +
                            "  cApprovd2 = '1'" +
                            ", sApprovd2 = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dApprovd2 = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);
        
        if (p_oApp.executeQuery(lsSQL, "Incentive_Master", p_sBranchCd, lsTransNox.substring(0, 4)) <= 0){
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public int getDeductionCount() throws SQLException{
        p_oDedctn.last();
        return p_oDedctn.getRow();
    }
    
    public int getDeductionEmployeeAllocationCount() throws SQLException{
        p_oDedctn_Emp.last();
        return p_oDedctn_Emp.getRow();
    }
    
    public int getItemCount() throws SQLException{
        p_oDetail.last();
        return p_oDetail.getRow();
    }
    
    public int getIncentiveCount() throws SQLException{
        p_oAllctn.last();
        return p_oAllctn.getRow();
    }
    
    public int getIncentiveEmployeeAllocationCount() throws SQLException{
        p_oAllctn_Emp.last();
        return p_oAllctn_Emp.getRow();
    }
    
    public Object getIncentiveInfo(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getIncentiveCount() == 0 || fnRow > getIncentiveCount()) return null;
        
        p_oAllctn.absolute(fnRow);
        switch (fnIndex){
            case 7://nInctvAmt
                return DecryptAmount(p_oAllctn.getString(fnIndex));
            case 101://xAllocPer
                switch (p_oAllctn.getString("xByPercnt")){
                    case "0": //no
                        return (getAllocatedIncentive(fnRow, "0") / DecryptAmount(p_oAllctn.getString("nInctvAmt"))) * 100;
                    case "1": //yes
                        return getAllocatedIncentive(fnRow, "1");
                    case "2":
                        return (getAllocatedIncentive(fnRow, "0") + ((DecryptAmount(p_oAllctn.getString("nInctvAmt")) - getAllocatedIncentive(fnRow, p_oAllctn.getString("xByPercnt"))) * getAllocatedIncentive(fnRow, "1") / 100)) / DecryptAmount(p_oAllctn.getString("nInctvAmt")) * 100;
                    default:
                        return 0.00;
                }
            case 102://xAllocAmt
                switch (p_oAllctn.getString("xByPercnt")){
                    case "0": //no
                        return getAllocatedIncentive(fnRow, p_oAllctn.getString("xByPercnt"));
                    case "1":
                        return getAllocatedIncentive(fnRow, p_oAllctn.getString("xByPercnt")) * DecryptAmount(p_oAllctn.getString("nInctvAmt")) / 100;
                    case "2": //combi
                        return getAllocatedIncentive(fnRow, p_oAllctn.getString("xByPercnt")) +
                                ((DecryptAmount(p_oAllctn.getString("nInctvAmt")) - getAllocatedIncentive(fnRow, p_oAllctn.getString("xByPercnt"))) * getAllocatedIncentive(fnRow, "1") / 100);
                    
                    default:
                        return 0.00;
                }
            default:
                return p_oAllctn.getObject(fnIndex);
        }
    }
    
    public Object getIncentiveInfo(int fnRow, String fsIndex) throws SQLException{
        return getIncentiveInfo(fnRow, getColumnIndex(p_oAllctn, fsIndex));
    }
    
    public void setIncentiveInfo(int fnRow, int fnIndex, Object foValue) throws SQLException{
        if (getIncentiveCount() == 0 || fnRow > getIncentiveCount()) return;
        
        p_oAllctn.absolute(fnRow);
        switch(fnIndex){
            case 3: //nQtyGoalx
            case 4: //nQtyActlx
                p_oAllctn.updateInt(fnIndex, 0);

                if (StringUtil.isNumeric(String.valueOf(foValue))) 
                    p_oAllctn.updateInt(fnIndex, (int) foValue);
                
                p_oAllctn.updateRow();   
                break;
            case 5: //nAmtGoalx
            case 6: //nAmtActlx
                p_oAllctn.updateDouble(fnIndex, 0.00);
                
                if (StringUtil.isNumeric(String.valueOf(foValue))) 
                    p_oAllctn.updateDouble(fnIndex, (double) foValue);
                
                p_oAllctn.updateRow();   
                break;
            case 7: //nInctvAmt
                if (StringUtil.isNumeric(String.valueOf(foValue))) 
                    p_oAllctn.updateString(fnIndex, EncryptAmount((double) foValue));
                else
                    p_oAllctn.updateString(fnIndex, EncryptAmount(0.00));

                p_oAllctn.updateRow();   
                    
                computeEmpTotalIncentiveAmount();
                break;
            case 8: //sRemarksx
            case 9: //xInctvNme
            case 10: //xByPercnt
                p_oAllctn.updateString(fnIndex, (String) foValue);
                p_oAllctn.updateRow();   
        }
        
    }
    
    public void setIncentiveInfo(int fnRow, String fsIndex, Object foValue) throws SQLException{
        setIncentiveInfo(fnRow, getColumnIndex(p_oAllctn, fsIndex), foValue);
    }
    
    public Object getIncentiveEmployeeAllocationInfo(int fnIndex, String fsInctveCD, String fsEmployID) throws SQLException{
        if (getItemCount() == 0 || getIncentiveCount() == 0) return null;
        
        //find record based on incentive code and employee id
        int lnRow = getIncentiveEmployeeAllocationCount();
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            p_oAllctn_Emp.absolute(lnCtr);
            
            if (fsInctveCD.equals(p_oAllctn_Emp.getString("sInctveCD")) &&
                fsEmployID.equals(p_oAllctn_Emp.getString("sEmployID"))){
                
                switch(fnIndex){
                    case 5: //nAllcAmtx
                        return DecryptAmount(p_oAllctn_Emp.getString(fnIndex));
                    default:
                        return p_oAllctn_Emp.getObject(fnIndex);
                }  
            }
        }
        
        return null;
    }
    
    public Object getIncentiveEmployeeAllocationInfo(String fsIndex, String fsInctveCD, String fsEmployID) throws SQLException{
        return getIncentiveEmployeeAllocationInfo(getColumnIndex(p_oAllctn_Emp, fsIndex), fsInctveCD, fsEmployID);
    }
    
    public void setDeductionEmployeeAllocationInfo(int fnIndex, int fnEntryNox, String fsEmployID, Object foValue) throws SQLException{
        if (getItemCount() == 0 || getDeductionCount()== 0 || fnIndex == 0) return;
        
        int lnCtr;
        
        //find record based on deduction entry no and employee id
        int lnRow = getDeductionEmployeeAllocationCount();
        
        for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            p_oDedctn_Emp.absolute(lnCtr);
            
            if (fnEntryNox == p_oDedctn_Emp.getInt("nEntryNox") &&
                fsEmployID.equals(p_oDedctn_Emp.getString("sEmployID"))){
                
                switch(fnIndex){
                    case 4: //nAllcPerc
                        p_oDedctn_Emp.updateDouble(fnIndex, 0.00);

                        if (StringUtil.isNumeric(String.valueOf(foValue))) 
                            p_oDedctn_Emp.updateDouble(fnIndex, (double) foValue);
                        
                        break;
                    case 5: //nAllcAmtx
                        if (StringUtil.isNumeric(String.valueOf(foValue))) 
                            p_oDedctn_Emp.updateString(fnIndex, EncryptAmount((double) foValue));
                        else
                            p_oDedctn_Emp.updateString(fnIndex, EncryptAmount(0.00));

                        break;
                    default:
                        p_oDedctn_Emp.setObject(fnIndex, (String) foValue);
                }  
                
                p_oDedctn_Emp.updateRow();
                computeEmpTotalIncentiveAmount();     
                break;
            }
        }
    }
    
    public void setDeductionEmployeeAllocationInfo(String fsIndex, int fnEntryNox, String fsEmployID, Object foValue) throws SQLException{
        setDeductionEmployeeAllocationInfo(getColumnIndex(p_oDedctn_Emp, fsIndex), fnEntryNox, fsEmployID, foValue);
    }
    
    public void setIncentiveEmployeeAllocationInfo(int fnIndex, String fsInctveCD, String fsEmployID, Object foValue) throws SQLException{
        setIncentiveEmployeeAllocationInfo(p_oAllctn_Emp.getMetaData().getColumnLabel(fnIndex), fsInctveCD, fsEmployID, foValue);
    }
    
    public void resetIncentiveEmployeeAllocation(String fsInctveCD) throws SQLException {
        if (getItemCount() == 0 || getIncentiveCount() == 0) return;
        
        int lnRow = getIncentiveEmployeeAllocationCount();
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            if (fsInctveCD.equals(p_oAllctn_Emp.getString("sInctveCD"))){
                p_oAllctn_Emp.absolute(lnCtr);
                p_oAllctn_Emp.updateDouble("nAllcPerc", 0.00);
                p_oAllctn_Emp.updateString("nAllcAmtx", EncryptAmount(0.00));
                p_oAllctn_Emp.updateRow();
            }
        }
        computeEmpTotalIncentiveAmount();
    }
    
    public void resetDeductionEmployeeAllocation(int fnEntryNox) throws SQLException{
        if (getItemCount() == 0 || getDeductionCount()== 0) return;
        
        int lnCtr;
        
        //find record based on deduction entry no and employee id
        int lnRow = getDeductionEmployeeAllocationCount();
        
        for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            p_oDedctn_Emp.absolute(lnCtr);
            
            if (fnEntryNox == p_oDedctn_Emp.getInt("nEntryNox")){
                p_oDedctn_Emp.absolute(lnCtr);
                p_oDedctn_Emp.updateDouble("nAllcPerc", 0.00);
                p_oDedctn_Emp.updateString("nAllcAmtx", EncryptAmount(0.00));
                p_oDedctn_Emp.updateRow();
            }
        }
        computeEmpTotalIncentiveAmount();
    }
    
    public void setIncentiveEmployeeAllocationInfo(String fsIndex, String fsInctveCD, String fsEmployID, Object foValue) throws SQLException{
        if (getItemCount() == 0 || getIncentiveCount() == 0) return;
        
        int lnCtr;
        int lnRow = getIncentiveCount();
        
        for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            p_oAllctn.absolute(lnCtr);
            if (fsInctveCD.equals(p_oAllctn.getString("sInctveCD"))) break;
        }
        
        //find record based on incentive code and employee id
        lnRow = getIncentiveEmployeeAllocationCount();
        
        for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            p_oAllctn_Emp.absolute(lnCtr);
            
            if (fsInctveCD.equals(p_oAllctn_Emp.getString("sInctveCD")) &&
                fsEmployID.equals(p_oAllctn_Emp.getString("sEmployID"))){
                
                switch(fsIndex){
                    case "nAllcPerc":
                        if (p_oAllctn.getString("xByPercnt").equals("1") ||
                            p_oAllctn.getString("xByPercnt").equals("2")){

                            p_oAllctn_Emp.updateDouble(fsIndex, 0.00);

                            if (StringUtil.isNumeric(String.valueOf(foValue))) 
                                p_oAllctn_Emp.updateDouble(fsIndex, (double) foValue);
                        }                 
                        break;
                    case "nAllcAmtx":
                        if (p_oAllctn.getString("xByPercnt").equals("0") ||
                            p_oAllctn.getString("xByPercnt").equals("2")){
                            
                            if (StringUtil.isNumeric(String.valueOf(foValue))) 
                                p_oAllctn_Emp.updateString(fsIndex, EncryptAmount((double) foValue));
                            else
                                p_oAllctn_Emp.updateString(fsIndex, EncryptAmount(0.00));
                        }                 
                        break;
                    case "sRemarksx":
                        p_oAllctn_Emp.setString(fsIndex, (String) foValue);
                }  
                
                p_oAllctn_Emp.updateRow();
                computeEmpTotalIncentiveAmount();
                break;
            }
        }
    }
    
    public Object getDeductionEmployeeAllocationInfo(int fnIndex, int fnEntryNox, String fsEmployID) throws SQLException{
        if (fnIndex == 0) return null;
        if (getItemCount() == 0 || getDeductionCount()== 0) return null;
        
        //find record based on incentive code and employee id
        int lnRow = getDeductionEmployeeAllocationCount();
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            p_oDedctn_Emp.absolute(lnCtr);
            
            if (fnEntryNox == p_oDedctn_Emp.getInt("nEntryNox") &&
                fsEmployID.equals(p_oDedctn_Emp.getString("sEmployID"))){
                
                switch(fnIndex){
                    case 5: //nAllcAmtx
                        return DecryptAmount(p_oDedctn_Emp.getString(fnIndex));
                    default:
                        return p_oDedctn_Emp.getObject(fnIndex);
                }  
            }
        }
        
        return null;
    }
    
    public Object getDeductionEmployeeAllocationInfo(String fsIndex, int fnEntryNox, String fsEmployID) throws SQLException{
        return getDeductionEmployeeAllocationInfo(getColumnIndex(p_oDedctn_Emp, fsIndex), fnEntryNox, fsEmployID);
    }
    
    public Object getDeductionInfo(int fnRow, int fnIndex) throws SQLException{
        if (fnRow == 0 || getDeductionCount() == 0) return null;
        
        p_oDedctn.absolute(fnRow);
        switch (fnIndex){
            case 4: //nDedctAmt
                return DecryptAmount(p_oDedctn.getString("nDedctAmt"));
            case 101: //xAllocPer
                return ((getAllocatedDeduction(fnRow, "0") + 
                        ((DecryptAmount(p_oDedctn.getString("nDedctAmt")) - getAllocatedDeduction(fnRow, "0")) * getAllocatedDeduction(fnRow, "1") / 100)) / DecryptAmount(p_oDedctn.getString("nDedctAmt")) * 100);
            case 102: //xAllocAmt
                return getAllocatedDeduction(fnRow, "0") +
                                ((DecryptAmount(p_oDedctn.getString("nDedctAmt")) - getAllocatedDeduction(fnRow, "0")) * getAllocatedDeduction(fnRow, "1") / 100);
            default:
                return p_oDedctn.getObject(fnIndex);
        }
    }
    
    public Object getDeductionInfo(int fnRow, String fsIndex) throws SQLException{
        switch (fsIndex){
            case "xAllocPer":
                return 101;
            case "xAllocAmt":
                return 102;
            default:
                return getDeductionInfo(fnRow, getColumnIndex(p_oDedctn, fsIndex));
        }
    }
    
    public void setDeductionInfo(int fnRow, int fnIndex, Object foValue) throws SQLException{
        if (getDeductionCount() == 0 || fnRow == 0) return;
        
        p_oDedctn.absolute(fnRow);
        
        switch (fnIndex){
            case 4: //nDedctAmt
                if (StringUtil.isNumeric(String.valueOf(foValue))) 
                    p_oDedctn.updateObject(fnIndex, EncryptAmount((double) foValue));
                else
                    p_oDedctn.updateObject(fnIndex, EncryptAmount(0.00));

                p_oDedctn.updateRow();   
                
                computeEmpTotalIncentiveAmount();
                break;
            default:
                p_oDedctn.updateObject(fnIndex, foValue);
                break;
        }
    }
    
    public void setDeductionInfo(int fnRow, String fsIndex, Object foValue) throws SQLException{
        setDeductionInfo(fnRow, getColumnIndex(p_oDedctn, fsIndex), foValue);
    }
    
    public Object getDetail(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getItemCount() == 0 || fnRow > getItemCount()) return null;
        
        p_oDetail.absolute(fnRow);
        switch (fnIndex){
            case 4://nTotalAmt
                return DecryptAmount(p_oDetail.getString(fnIndex));
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
            case 4://sMonthxxx
            case 5://sRemarksx
            case 16://xBranchNm
            case 17://xDeptName
                p_oMaster.updateString(fnIndex, (String) foValue);
                p_oMaster.updateRow();

                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oMaster.getString(fnIndex));
                break;
        }
    }
    
    public void setMaster(String fsIndex, Object foValue) throws SQLException{
        setMaster(getColumnIndex(p_oMaster, fsIndex), foValue);
    }
    
    public boolean removeDetail(int fnRow) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW) {
            p_sMessage = "This feature was only for new entries.";
            return false;
        }
        
        if (fnRow > getItemCount()) return false;
        
        if (getIncentiveCount() > 0){
            p_sMessage = "Unable to remove an employee when an incentive was already added.";
            return false;
        }
        
        p_oDetail.absolute(fnRow);
        p_oDetail.deleteRow();
        
        return true;
    }
    
    public boolean removeIncentive(String fsValue) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW) {
            p_sMessage = "This feature was only for new entries.";
            return false;
        }

        int lnCtr;
        int lnRow = getIncentiveCount();
        
        for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            if (fsValue.equals((String) getIncentiveInfo(lnCtr, "sInctveCD"))){
                p_oAllctn.absolute(lnCtr);
                p_oAllctn.deleteRow();
                break;
            }
        }
        
        boolean lbSearch = true;
        while (lbSearch){
            lnRow = getIncentiveEmployeeAllocationCount();
            
            for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                p_oAllctn_Emp.absolute(lnCtr);
                if (fsValue.equals(p_oAllctn_Emp.getString("sInctveCD"))){
                    p_oAllctn_Emp.deleteRow();
                } 
                
                if (lnCtr == lnRow) lbSearch = false;
            }
        }
        
        return true;
    }  
    
    public boolean removeDeduction(int fnEntryNox) throws SQLException{
         if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) {
            if (!p_oApp.getDepartment().equals(AUDITOR)){
                p_sMessage = "This feature was only for new entries.";
                return false;
            
            }
        }
        
        
        int lnCtr;
        int lnRow = getDeductionCount();
        
        for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            if ((int) getDeductionInfo(lnCtr, "nEntryNox") == fnEntryNox){
                p_oDedctn.absolute(lnCtr);
                p_oDedctn.deleteRow();
                break;
            }
        }
        
        boolean lbSearch = true;
        while (lbSearch){
            lnRow = getDeductionEmployeeAllocationCount();
            
            for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                p_oDedctn_Emp.absolute(lnCtr);
                if (p_oDedctn_Emp.getInt("nEntryNox") == fnEntryNox){
                    p_oDedctn_Emp.deleteRow();
                } 
                if (lnCtr == lnRow) lbSearch = false;
            }
        }
        
        return true;
    }  
    
    public boolean searchIncentive(String fsValue, boolean fbByCode) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW) {
            p_sMessage = "This feature was only for new entries.";
            return false;
        }
        
        int lnCtr;
        int lnRow = getIncentiveCount();
        
        if (fbByCode){
            for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                if (fsValue.equals((String) getIncentiveInfo(lnCtr, "sInctveCD"))) return true;
            }
        } else{
            for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                if (fsValue.equals((String) getIncentiveInfo(lnCtr, "xInctvNme"))) return true;
            }
        }

        String lsSQL = "SELECT" +
                            "  sInctveCD" +
                            ", sInctveDs" +
                            ", sDivision" +
                            ", cInctveTp" +
                            ", cByPercnt" +
                        " FROM Incentive" +
                        " WHERE cRecdStat = '1'";
        
        JSONObject loJSON;
        
        if (p_bWithUI){
            loJSON = showFXDialog.jsonSearch(
                        p_oApp, 
                        lsSQL, 
                        fsValue, 
                        "ID»Description", 
                        "sInctveCD»sInctveDs", 
                        "sInctveCD»sInctveDs", 
                        fbByCode ? 0 : 1);
            
            if (loJSON != null)
                return addIncentive((String) loJSON.get("sInctveCD"), (String) loJSON.get("sInctveDs"), (String) loJSON.get("cByPercnt"));
            else
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
        
        if (loJSON != null)
            return addIncentive((String) loJSON.get("sInctveCD"), (String) loJSON.get("sInctveDs"), (String) loJSON.get("cByPercnt"));
        else
            return false;
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
                System.out.println("sDeptIDxx = " +(String) loJSON.get("sDeptIDxx"));
                System.out.println("sDeptName = " +(String) loJSON.get("sDeptName"));
                p_oMaster.updateString("sDeptIDxx", (String) loJSON.get("sDeptIDxx"));
                p_oMaster.updateString("xDeptName", (String) loJSON.get("sDeptName"));
                p_oMaster.updateRow();
                
                //recreate detail and other tables
                createDetail();
                createDetailAllocation();
                createDetailAllocationEmp();
                createDetailDeductionAlloc();
                createDetailDeductionAllocEmp();

                if (p_oListener != null) p_oListener.MasterRetreive(17, getMaster("xDeptName"));
                
                return true;
            }
            
            p_oMaster.updateString("sDeptIDxx", "");
            p_oMaster.updateString("xDeptName", "");
            p_oMaster.updateRow();
                        
            //recreate detail and other tables
            createDetail();
            createDetailAllocation();
            createDetailAllocationEmp();
            createDetailDeductionAlloc();
            createDetailDeductionAllocEmp();
            
            if (p_oListener != null) p_oListener.MasterRetreive(17, "");
            
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

        if (p_oListener != null) p_oListener.MasterRetreive(17, p_oMaster.getString("xDeptName"));
        
        //recreate detail and other tables
        createDetail();
        createDetailAllocation();
        createDetailAllocationEmp();
        createDetailDeductionAlloc();
        createDetailDeductionAllocEmp();
        
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
    
    public void displayDetAllocFields() throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) return;
        
        int lnRow = p_oAllctn.getMetaData().getColumnCount();
        
        System.out.println("----------------------------------------");
        System.out.println("DETAIL ALLOCATION TABLE INFO");
        System.out.println("----------------------------------------");
        System.out.println("Total number of columns: " + lnRow);
        System.out.println("----------------------------------------");
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            System.out.println("Column index: " + (lnCtr) + " --> Label: " + p_oAllctn.getMetaData().getColumnLabel(lnCtr));
            if (p_oAllctn.getMetaData().getColumnType(lnCtr) == Types.CHAR ||
                p_oAllctn.getMetaData().getColumnType(lnCtr) == Types.VARCHAR){
                
                System.out.println("Column index: " + (lnCtr) + " --> Size: " + p_oAllctn.getMetaData().getColumnDisplaySize(lnCtr));
            }
        }
        
        System.out.println("----------------------------------------");
        System.out.println("END: DETAIL ALLOCATION TABLE INFO");
        System.out.println("----------------------------------------");
    }
    
    public void displayDetAllocEmpFields() throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) return;
        
        int lnRow = p_oAllctn_Emp.getMetaData().getColumnCount();
        
        System.out.println("----------------------------------------");
        System.out.println("DETAIL EMPLOYEE ALLOCATION TABLE INFO");
        System.out.println("----------------------------------------");
        System.out.println("Total number of columns: " + lnRow);
        System.out.println("----------------------------------------");
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            System.out.println("Column index: " + (lnCtr) + " --> Label: " + p_oAllctn_Emp.getMetaData().getColumnLabel(lnCtr));
            if (p_oAllctn_Emp.getMetaData().getColumnType(lnCtr) == Types.CHAR ||
                p_oAllctn_Emp.getMetaData().getColumnType(lnCtr) == Types.VARCHAR){
                
                System.out.println("Column index: " + (lnCtr) + " --> Size: " + p_oAllctn_Emp.getMetaData().getColumnDisplaySize(lnCtr));
            }
        }
        
        System.out.println("----------------------------------------");
        System.out.println("END: DETAIL EMPLOYEE ALLOCATION TABLE INFO");
        System.out.println("----------------------------------------");
    }
    
    public void displayDetDeductionAllocFields() throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) return;
        
        int lnRow = p_oDedctn.getMetaData().getColumnCount();
        
        System.out.println("----------------------------------------");
        System.out.println("DETAIL DEDUCTION ALLOCATION TABLE INFO");
        System.out.println("----------------------------------------");
        System.out.println("Total number of columns: " + lnRow);
        System.out.println("----------------------------------------");
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            System.out.println("Column index: " + (lnCtr) + " --> Label: " + p_oDedctn.getMetaData().getColumnLabel(lnCtr));
            if (p_oDedctn.getMetaData().getColumnType(lnCtr) == Types.CHAR ||
                p_oDedctn.getMetaData().getColumnType(lnCtr) == Types.VARCHAR){
                
                System.out.println("Column index: " + (lnCtr) + " --> Size: " + p_oDedctn.getMetaData().getColumnDisplaySize(lnCtr));
            }
        }
        
        System.out.println("----------------------------------------");
        System.out.println("END: DETAIL DEDUCTION ALLOCATION TABLE INFO");
        System.out.println("----------------------------------------");
    }
    
    public void displayDetDeductionAllocEmpFields() throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) return;
        
        int lnRow = p_oDedctn.getMetaData().getColumnCount();
        
        System.out.println("----------------------------------------");
        System.out.println("DETAIL EMPLOYEE DEDUCTION ALLOCATION TABLE INFO");
        System.out.println("----------------------------------------");
        System.out.println("Total number of columns: " + lnRow);
        System.out.println("----------------------------------------");
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            System.out.println("Column index: " + (lnCtr) + " --> Label: " + p_oDedctn_Emp.getMetaData().getColumnLabel(lnCtr));
            if (p_oDedctn_Emp.getMetaData().getColumnType(lnCtr) == Types.CHAR ||
                p_oDedctn_Emp.getMetaData().getColumnType(lnCtr) == Types.VARCHAR){
                
                System.out.println("Column index: " + (lnCtr) + " --> Size: " + p_oDedctn_Emp.getMetaData().getColumnDisplaySize(lnCtr));
            }
        }
        
        System.out.println("----------------------------------------");
        System.out.println("END: DETAIL EMPLOYEE DEDUCTION ALLOCATION TABLE INFO");
        System.out.println("----------------------------------------");
    }
    
    private void computeEmpTotalIncentiveAmount() throws SQLException{
        int lnDetRow = getItemCount();
        int lnIncRow;
        int lnAlcRow;
        
        int lnCtr1, lnCtr2, lnCtr3;
        double lnTotalAmt, lnPercentx, lnAllcAmtx, lnIncentve, lnDeductnx;
        double lnTotalInc, lnTotalDed;       
        
        for (lnCtr1 = 1; lnCtr1 <= lnDetRow; lnCtr1++){
            p_oDetail.absolute(lnCtr1);
            
            lnTotalInc = 0.00;
            lnTotalDed = 0.00;
            lnTotalAmt = 0.00;
            
            //incentive
            lnIncRow = getIncentiveCount();
            lnAlcRow = getIncentiveEmployeeAllocationCount();
            for (lnCtr2 = 1; lnCtr2 <= lnIncRow; lnCtr2++){
                p_oAllctn.absolute(lnCtr2);
                
                if (p_oAllctn.getString("xByPercnt").equals("2"))
                    lnAllcAmtx = getAllocatedIncentive(lnCtr2, "2");
                else
                    lnAllcAmtx = 0.00;
                
                for (lnCtr3 = 1; lnCtr3 <= lnAlcRow; lnCtr3++){
                    p_oAllctn_Emp.absolute(lnCtr3);
                    
                    lnIncentve = 0.00;
                    if (p_oAllctn.getString("sInctveCD").equals(p_oAllctn_Emp.getString("sInctveCD")) &&
                        p_oDetail.getString("sEmployID").equals(p_oAllctn_Emp.getString("sEmployID"))){
                        
                        switch (p_oAllctn.getString("xByPercnt")){
                            case "0":
                                lnIncentve = DecryptAmount(p_oAllctn_Emp.getString("nAllcAmtx"));
                                
                                lnTotalAmt += lnIncentve;
                                lnTotalAmt = lnTotalAmt * 100 / 100; //round off
                                break;
                            case "1":
                                lnPercentx = p_oAllctn_Emp.getDouble("nAllcPerc") / 100;
                                lnPercentx = lnPercentx * DecryptAmount(p_oAllctn.getString("nInctvAmt"));
                                
                                lnIncentve = lnPercentx * 100 / 100;
                                
                                lnTotalAmt += lnIncentve;
                                lnTotalAmt = lnTotalAmt * 100 / 100; //round off
                                break;
                            case "2": 
                                lnIncentve = DecryptAmount(p_oAllctn_Emp.getString("nAllcAmtx"));
                                
                                lnPercentx = p_oAllctn_Emp.getDouble("nAllcPerc") / 100;
                                lnPercentx = lnPercentx * (DecryptAmount(p_oAllctn.getString("nInctvAmt")) - lnAllcAmtx);
                                
                                lnIncentve += lnPercentx * 100 / 100;
                                
                                lnTotalAmt += lnIncentve;
                                lnTotalAmt = lnTotalAmt * 100 / 100; //round off
                                break;
                        }                
                        
                        p_oAllctn_Emp.updateObject("nTotalAmt", lnIncentve);
                        p_oAllctn_Emp.updateRow();
                        
                        lnTotalInc += lnIncentve;
                    } 
                }
            }
            
            //deductions
            lnIncRow = getDeductionCount();
            lnAlcRow = getDeductionEmployeeAllocationCount();
            for (lnCtr2 = 1; lnCtr2 <= lnIncRow; lnCtr2++){
                p_oDedctn.absolute(lnCtr2);
                
                lnAllcAmtx = getAllocatedDeduction(lnCtr2, "2");
                
                for (lnCtr3 = 1; lnCtr3 <= lnAlcRow; lnCtr3++){
                    p_oDedctn_Emp.absolute(lnCtr3);
                    
                    lnDeductnx = 0.00;
                    if (p_oDedctn.getInt("nEntryNox") == p_oDedctn_Emp.getInt("nEntryNox") &&
                        p_oDetail.getString("sEmployID").equals(p_oDedctn_Emp.getString("sEmployID"))){
                        
                        lnDeductnx = DecryptAmount(p_oDedctn_Emp.getString("nAllcAmtx"));

                        lnPercentx = p_oDedctn_Emp.getDouble("nAllcPerc") / 100;
                        lnPercentx = lnPercentx * (DecryptAmount(p_oDedctn.getString("nDedctAmt")) - lnAllcAmtx);

                        lnDeductnx += lnPercentx * 100 / 100;
                        
                        lnTotalAmt -= lnDeductnx;
                        lnTotalAmt = lnTotalAmt * 100 / 100; //round off
                        
                        p_oDedctn_Emp.updateObject("nTotalAmt", lnDeductnx);
                        p_oDedctn_Emp.updateRow();
                        
                        lnTotalDed += lnDeductnx;
                        break;
                    } 
                }
            }
            
            p_oDetail.updateDouble("xIncentve", lnTotalInc);
            p_oDetail.updateDouble("xDeductnx", lnTotalDed);
            p_oDetail.updateString("nTotalAmt", EncryptAmount(lnTotalInc - lnTotalDed));
            p_oDetail.updateRow();
        }
        
        if (p_oListener != null) p_oListener.DetailRetreive(0, 0, "");
    }
    
    private double getAllocatedIncentive(int fnRow, String fcByPercnt) throws SQLException{        
        int lnCtr;
        int lnRow = getIncentiveEmployeeAllocationCount();
        double lnAllocated = 0.00;
        
        String lsInctveCD = (String) getIncentiveInfo(fnRow, "sInctveCD");
        
        switch (fcByPercnt){
            case "0":
            case "2":
                for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                    p_oAllctn_Emp.absolute(lnCtr);
                    if (lsInctveCD.equals(p_oAllctn_Emp.getString("sInctveCD")))
                        lnAllocated += DecryptAmount(p_oAllctn_Emp.getString("nAllcAmtx"));
                }
                break;
            case "1":
                for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                    p_oAllctn_Emp.absolute(lnCtr);
                    if (lsInctveCD.equals(p_oAllctn_Emp.getString("sInctveCD")))
                        lnAllocated += p_oAllctn_Emp.getDouble("nAllcPerc");
                }
        }

        return lnAllocated;
    }
    
    private double getAllocatedDeduction(int fnRow, String fcByPercnt) throws SQLException{
        int lnCtr;
        int lnRow = getDeductionEmployeeAllocationCount();
        double lnAllocated = 0.00;
        
        switch (fcByPercnt){
            case "0":
            case "2":
                for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                    p_oDedctn_Emp.absolute(lnCtr);
                    if (fnRow == p_oDedctn_Emp.getInt("nEntryNox"))
                        lnAllocated += DecryptAmount(p_oDedctn_Emp.getString("nAllcAmtx"));
                }
                break;
            case "1":
                for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                    p_oDedctn_Emp.absolute(lnCtr);
                    if (fnRow == p_oDedctn_Emp.getInt("nEntryNox"))
                        lnAllocated += p_oDedctn_Emp.getDouble("nAllcPerc");
                }
        }

        return lnAllocated;
    }
    
    private boolean addIncentive(String fsCode, String fsDescript, String fcByPercnt) throws SQLException{
        int lnCtr;
        int lnRow = getIncentiveCount();
        
        //validate if incentive is already added
        for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            if (fsCode.equals((String) getIncentiveInfo(lnCtr, "sInctveCD"))) return true;
        }
        
        p_oAllctn.last();
        p_oAllctn.moveToInsertRow();

        MiscUtil.initRowSet(p_oAllctn);     
        p_oAllctn.updateString("sInctveCD", fsCode);
        p_oAllctn.updateString("xInctvNme", fsDescript);
        p_oAllctn.updateString("xByPercnt", fcByPercnt);
        p_oAllctn.updateString("nInctvAmt", EncryptAmount(0.00));

        p_oAllctn.insertRow();
        p_oAllctn.moveToCurrentRow();
        
        //add employee allocation for this incentive
        lnRow = getItemCount();
        
        for (lnCtr = 1; lnCtr <= lnRow; lnCtr ++){
            p_oAllctn_Emp.last();
            p_oAllctn_Emp.moveToInsertRow();

            MiscUtil.initRowSet(p_oAllctn_Emp);     
            p_oAllctn_Emp.updateString("sEmployID", (String) getDetail(lnCtr, "sEmployID"));
            p_oAllctn_Emp.updateString("sInctveCD", fsCode);
            p_oAllctn_Emp.updateString("nAllcAmtx", EncryptAmount(0.00));

            p_oAllctn_Emp.insertRow();
            p_oAllctn_Emp.moveToCurrentRow();
        }
        
        return true;
    }
    
    public boolean addDeduction(String fsRemarksx) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) {
            if (!p_oApp.getDepartment().equals(AUDITOR)){
                p_sMessage = "This feature was only for new entries.";
                return false;
            
            }
        }
        
        fsRemarksx = fsRemarksx.trim().toUpperCase();
        
        //check if the deduction already exists
        int lnRox = getDeductionCount();
        
        if (lnRox > 0){
            p_oDedctn.beforeFirst();
            
            while (p_oDedctn.next()){
                if (p_oDedctn.getString("sRemarksx").toUpperCase().equals(fsRemarksx)) 
                    return true;
            }            
        }
        
        //add the deduction
        p_oDedctn.last();
        p_oDedctn.moveToInsertRow();
        MiscUtil.initRowSet(p_oDedctn);     
        p_oDedctn.updateObject("nEntryNox", lnRox + 1);
        p_oDedctn.updateObject("sRemarksx", fsRemarksx);
        p_oDedctn.updateObject("xNewValue", "0");
        p_oDedctn.updateObject("nDedctAmt", EncryptAmount(0.00));
        p_oDedctn.insertRow();
        p_oDedctn.moveToCurrentRow();
        
        //add the employees
        int lnRow = getItemCount();
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr ++){
            p_oDedctn_Emp.last();
            p_oDedctn_Emp.moveToInsertRow();

            MiscUtil.initRowSet(p_oDedctn_Emp);     
            p_oDedctn_Emp.updateObject("sEmployID", (String) getDetail(lnCtr, "sEmployID"));
            p_oDedctn_Emp.updateObject("nEntryNox", lnRox + 1);
            p_oDedctn_Emp.updateString("nAllcAmtx", EncryptAmount(0.00));
            p_oDedctn_Emp.updateObject("nNewValue", "0");
            p_oDedctn_Emp.updateString("xEmployNm", (String) getDetail(lnCtr, "xEmployNm"));
            p_oDedctn_Emp.insertRow();
            p_oDedctn_Emp.moveToCurrentRow();
        }
        
        return true;
    }
    
    private void createDetailDeductionAllocEmp() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();        

        meta.setColumnCount(8);
        
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
        
        meta.setColumnName(4, "nAllcPerc");
        meta.setColumnLabel(4, "nAllcPerc");
        meta.setColumnType(4, Types.DOUBLE);
        
        meta.setColumnName(5, "nAllcAmtx");
        meta.setColumnLabel(5, "nAllcAmtx");
        meta.setColumnType(5, Types.VARCHAR);
        meta.setColumnDisplaySize(5, 32);
        
        meta.setColumnName(6, "xEmployNm");
        meta.setColumnLabel(6, "xEmployNm");
        meta.setColumnType(6, Types.VARCHAR);
        
        meta.setColumnName(7, "nNewValue");
        meta.setColumnLabel(7, "nNewValue");
        meta.setColumnType(7, Types.VARCHAR);
        
        meta.setColumnName(8, "nTotalAmt");
        meta.setColumnLabel(8, "nTotalAmt");
        meta.setColumnType(8, Types.DOUBLE);
        
        p_oDedctn_Emp = new CachedRowSetImpl();
        p_oDedctn_Emp.setMetaData(meta);        
    }
    
    private void createDetailDeductionAlloc() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();        

        meta.setColumnCount(5);
        
        meta.setColumnName(1, "sTransNox");
        meta.setColumnLabel(1, "sTransNox");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);
        
        meta.setColumnName(2, "nEntryNox");
        meta.setColumnLabel(2, "nEntryNox");
        meta.setColumnType(2, Types.INTEGER);
        
        meta.setColumnName(3, "sRemarksx");
        meta.setColumnLabel(3, "sRemarksx");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 64);
        
        meta.setColumnName(4, "nDedctAmt");
        meta.setColumnLabel(4, "nDedctAmt");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 32);
        
        meta.setColumnName(5, "xNewValue");
        meta.setColumnLabel(5, "xNewValue");
        meta.setColumnType(5, Types.VARCHAR);
        meta.setColumnDisplaySize(5, 1);
        
        p_oDedctn = new CachedRowSetImpl();
        p_oDedctn.setMetaData(meta);
    }
    
    private void createDetailAllocationEmp() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();        

        meta.setColumnCount(9);
        
        meta.setColumnName(1, "sTransNox");
        meta.setColumnLabel(1, "sTransNox");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);
        
        meta.setColumnName(2, "sEmployID");
        meta.setColumnLabel(2, "sEmployID");
        meta.setColumnType(2, Types.VARCHAR);
        meta.setColumnDisplaySize(2, 12);
        
        meta.setColumnName(3, "sInctveCD");
        meta.setColumnLabel(3, "sInctveCD");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 3);
        
        meta.setColumnName(4, "nAllcPerc");
        meta.setColumnLabel(4, "nAllcPerc");
        meta.setColumnType(4, Types.DOUBLE);
        
        meta.setColumnName(5, "nAllcAmtx");
        meta.setColumnLabel(5, "nAllcAmtx");
        meta.setColumnType(5, Types.VARCHAR);
        meta.setColumnDisplaySize(5, 32);
        
        meta.setColumnName(6, "xEmployNm");
        meta.setColumnLabel(6, "xEmployNm");
        meta.setColumnType(6, Types.VARCHAR);
        
        meta.setColumnName(7, "xInctvNme");
        meta.setColumnLabel(7, "xInctvNme");
        meta.setColumnType(7, Types.VARCHAR);        
        
        meta.setColumnName(8, "nTotalAmt");
        meta.setColumnLabel(8, "nTotalAmt");
        meta.setColumnType(8, Types.DOUBLE);      
        
        meta.setColumnName(9, "nNewValue");
        meta.setColumnLabel(9, "nNewValue");
        meta.setColumnType(9, Types.VARCHAR);
        meta.setColumnDisplaySize(9, 1);
        
        
        p_oAllctn_Emp = new CachedRowSetImpl();
        p_oAllctn_Emp.setMetaData(meta);
    }
    
    private void createDetailAllocation() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();        

        meta.setColumnCount(10);
        
        meta.setColumnName(1, "sTransNox");
        meta.setColumnLabel(1, "sTransNox");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);
        
        meta.setColumnName(2, "sInctveCD");
        meta.setColumnLabel(2, "sInctveCD");
        meta.setColumnType(2, Types.VARCHAR);
        meta.setColumnDisplaySize(2, 3);
        
        meta.setColumnName(3, "nQtyGoalx");
        meta.setColumnLabel(3, "nQtyGoalx");
        meta.setColumnType(3, Types.INTEGER);
        
        meta.setColumnName(4, "nQtyActlx");
        meta.setColumnLabel(4, "nQtyActlx");
        meta.setColumnType(4, Types.INTEGER);
        
        meta.setColumnName(5, "nAmtGoalx");
        meta.setColumnLabel(5, "nAmtGoalx");
        meta.setColumnType(5, Types.DOUBLE);
        
        meta.setColumnName(6, "nAmtActlx");
        meta.setColumnLabel(6, "nAmtActlx");
        meta.setColumnType(6, Types.DOUBLE);
        
        meta.setColumnName(7, "nInctvAmt");
        meta.setColumnLabel(7, "nInctvAmt");
        meta.setColumnType(7, Types.VARCHAR);
        meta.setColumnDisplaySize(7, 32);

        meta.setColumnName(8, "sRemarksx");
        meta.setColumnLabel(8, "sRemarksx");
        meta.setColumnType(8, Types.VARCHAR);
        meta.setColumnDisplaySize(8, 64);
        
        meta.setColumnName(9, "xInctvNme");
        meta.setColumnLabel(9, "xInctvNme");
        meta.setColumnType(9, Types.VARCHAR);        
        
        meta.setColumnName(10, "xByPercnt");
        meta.setColumnLabel(10, "xByPercnt");
        meta.setColumnType(10, Types.VARCHAR);        
        
        p_oAllctn = new CachedRowSetImpl();
        p_oAllctn.setMetaData(meta);
    }
    
    private void createDetail() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();        

        meta.setColumnCount(10);
        
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
        
        meta.setColumnName(4, "nTotalAmt");
        meta.setColumnLabel(4, "nTotalAmt");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 32);
        
        meta.setColumnName(5, "xEmployNm");
        meta.setColumnLabel(5, "xEmployNm");
        meta.setColumnType(5, Types.VARCHAR);
        
        meta.setColumnName(6, "xEmpLevNm");
        meta.setColumnLabel(6, "xEmpLevNm");
        meta.setColumnType(6, Types.VARCHAR);
        
        meta.setColumnName(7, "xPositnNm");
        meta.setColumnLabel(7, "xPositnNm");
        meta.setColumnType(7, Types.VARCHAR);
        
        meta.setColumnName(8, "xSrvcYear");
        meta.setColumnLabel(8, "xSrvcYear");
        meta.setColumnType(8, Types.VARCHAR);
        
        meta.setColumnName(9, "xIncentve");
        meta.setColumnLabel(9, "xIncentve");
        meta.setColumnType(9, Types.DOUBLE);
        
        meta.setColumnName(10, "xDeductnx");
        meta.setColumnLabel(10, "xDeductnx");
        meta.setColumnType(10, Types.DOUBLE);
        
        p_oDetail = new CachedRowSetImpl();
        p_oDetail.setMetaData(meta);        
        
        String lsSQL = "SELECT " +
        "      a.sEmployID " +
        "    , IFNULL(b.sCompnyNm, '') xEmployNm " +
        "    , IFNULL(c.sEmpLevNm, '') xEmpLevNm " +
        "    , IFNULL(d.sPositnNm, '') xPositnNm " +
        "    , IFNULL(a.sEmpLevID, '') xEmpLevID " +
        "    , IFNULL(a.sDeptIDxx, '') sDeptIDxx " +
        "    , IFNULL(ROUND(DATEDIFF(NOW(), IFNULL(a.dStartEmp, a.dHiredxxx)) / 365), '') xSrvcYear " +
        " FROM Employee_Master001 a " +
        "     LEFT JOIN Client_Master b ON a.sEmployID = b.sClientID " +
        "     LEFT JOIN Employee_Level c ON a.sEmpLevID = c.sEmpLevID " +
        "     LEFT JOIN `Position` d ON a.sPositnID = d.sPositnID " +
        " WHERE a.sBranchCd = " + SQLUtil.toSQL(p_sBranchCd) +
        "     AND a.cRecdStat = '1' " +
        "     AND ISNULL(a.dFiredxxx) ";
      
        String lsSQL2 =   " UNION SELECT" +
        "      h.sEmployID " +
        "    , IFNULL(i.sCompnyNm, '') xEmployNm " +
        "    , IFNULL(j.sEmpLevNm, '') xEmpLevNm " +
        "    , IFNULL(k.sPositnNm, '') xPositnNm " +
        "    , IFNULL(h.sEmpLevID, '') xEmpLevID " +
        "    , IFNULL(h.sDeptIDxx, '') sDeptIDxx " +
        "    , IFNULL(ROUND(DATEDIFF(NOW(), IFNULL(h.dStartEmp, h.dHiredxxx)) / 365), '') xSrvcYear" +
        "     FROM  Branch e" +
        "	 ,Branch_Others f " +
        "	 , Branch_Area g " +
        "     LEFT JOIN Employee_Master001 h " +
        "	ON g.sAreaMngr = h.sEmployID   " +
        "     LEFT JOIN Client_Master i ON h.sEmployID = i.sClientID " +
        "     LEFT JOIN Employee_Level j ON h.sEmpLevID = j.sEmpLevID " +
        "     LEFT JOIN `Position` k ON h.sPositnID = k.sPositnID" +
        "	WHERE e.sBranchCd = f.sBranchCd" +
        "	AND  f.sAreaCode = g.sAreaCode" +
        "	AND e.sBranchCd = " + SQLUtil.toSQL(p_sBranchCd) +
        " ORDER BY xEmpLevID DESC, xEmployNm";   
        p_oMaster.first();
        if (!p_oMaster.getString("sDeptIDxx").isEmpty())
            lsSQL = MiscUtil.addCondition(lsSQL, "sDeptIDxx = " + SQLUtil.toSQL(p_oMaster.getString("sDeptIDxx")));
            lsSQL = lsSQL + lsSQL2;
            lsSQL = MiscUtil.addCondition(lsSQL, "sDeptIDxx = " + SQLUtil.toSQL(p_oMaster.getString("sDeptIDxx")));
            
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        int lnRow = 1;
        while (loRS.next()){
            p_oDetail.last();
            p_oDetail.moveToInsertRow();

            MiscUtil.initRowSet(p_oDetail);        
            p_oDetail.updateInt("nEntryNox", lnRow);
            p_oDetail.updateString("sEmployID", loRS.getString("sEmployID"));
            p_oDetail.updateString("xEmployNm", loRS.getString("xEmployNm"));
            p_oDetail.updateString("xEmpLevNm", loRS.getString("xEmpLevNm"));
            p_oDetail.updateString("xPositnNm", loRS.getString("xPositnNm"));
            p_oDetail.updateString("xSrvcYear", loRS.getString("xSrvcYear"));
            p_oDetail.updateString("nTotalAmt", EncryptAmount(0.00));
            p_oDetail.updateDouble("xIncentve", 0.00);
            p_oDetail.updateDouble("xDeductnx", 0.00);
            
            
            p_oDetail.insertRow();
            p_oDetail.moveToCurrentRow();
            
            lnRow++;
        }
        System.out.println("count = " + lnRow);
    }
    
    private void createMaster() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(17);

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

        meta.setColumnName(4, "sMonthxxx");
        meta.setColumnLabel(4, "sMonthxxx");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 6);
        
        meta.setColumnName(5, "sRemarksx");
        meta.setColumnLabel(5, "sRemarksx");
        meta.setColumnType(5, Types.VARCHAR);
        meta.setColumnDisplaySize(5, 128);
        
        meta.setColumnName(6, "sPrepared");
        meta.setColumnLabel(6, "sPrepared");
        meta.setColumnType(6, Types.VARCHAR);
        meta.setColumnDisplaySize(6, 10);
        
        meta.setColumnName(7, "dPrepared");
        meta.setColumnLabel(7, "dPrepared");
        meta.setColumnType(7, Types.DATE);
        
        meta.setColumnName(8, "cApprovd1");
        meta.setColumnLabel(8, "cApprovd1");
        meta.setColumnType(8, Types.CHAR);
        meta.setColumnDisplaySize(8, 1);
        
        meta.setColumnName(9, "sApprovd1");
        meta.setColumnLabel(9, "sApprovd1");
        meta.setColumnType(9, Types.VARCHAR);
        meta.setColumnDisplaySize(9, 10);
        
        meta.setColumnName(10, "dApprovd1");
        meta.setColumnLabel(10, "dApprovd1");
        meta.setColumnType(10, Types.DATE);
        
        meta.setColumnName(11, "cApprovd2");
        meta.setColumnLabel(11, "cApprovd2");
        meta.setColumnType(11, Types.CHAR);
        meta.setColumnDisplaySize(11, 1);
        
        meta.setColumnName(12, "sApprovd2");
        meta.setColumnLabel(12, "sApprovd2");
        meta.setColumnType(12, Types.VARCHAR);
        meta.setColumnDisplaySize(12, 10);
        
        meta.setColumnName(13, "dApprovd2");
        meta.setColumnLabel(13, "dApprovd2");
        meta.setColumnType(13, Types.DATE);
        
        meta.setColumnName(14, "sBatchNox");
        meta.setColumnLabel(14, "sBatchNox");
        meta.setColumnType(14, Types.VARCHAR);
        meta.setColumnDisplaySize(14, 12);
        
        meta.setColumnName(15, "cTranStat");
        meta.setColumnLabel(15, "cTranStat");
        meta.setColumnType(15, Types.VARCHAR);
        meta.setColumnDisplaySize(15, 1);
        
        meta.setColumnName(16, "xBranchNm");
        meta.setColumnLabel(16, "xBranchNm");
        meta.setColumnType(16, Types.VARCHAR);
        
        meta.setColumnName(17, "xDeptName");
        meta.setColumnLabel(17, "xDeptName");
        meta.setColumnType(17, Types.VARCHAR);
        
        p_oMaster = new CachedRowSetImpl();
        p_oMaster.setMetaData(meta);
        
        p_oMaster.last();
        p_oMaster.moveToInsertRow();
        
        MiscUtil.initRowSet(p_oMaster);       
        
        p_oMaster.updateObject("sTransNox", MiscUtil.getNextCode("Incentive_Master", "sTransNox", true, p_oApp.getConnection(), p_sBranchCd));
        p_oMaster.updateObject("dTransact", p_oApp.getServerDate());
        p_oMaster.updateObject("cApprovd1", "0");
        p_oMaster.updateObject("cApprovd2", "0");
        p_oMaster.updateObject("cTranStat", TransactionStatus.STATE_OPEN);
        p_oMaster.updateObject("xBranchNm", p_oApp.getBranchName());
        
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
        if (p_oMaster.getString("sMonthxxx").isEmpty()){
            p_sMessage = "Period must not be empty.";
            return false;
        }
        
        //validate detail
        if (getItemCount() == 0){
            p_sMessage = "No employee detected.";
            return false;
        }
        
        p_oDetail.beforeFirst();
        while (p_oDetail.next()){            
            if (DecryptAmount(p_oDetail.getString("nTotalAmt")) < 0.00){
                p_sMessage = p_oDetail.getString("xEmployNm") + " has negative incentive total amount.";
                return false;
            }    
        }
        
        //validate incentive
        if (getIncentiveCount() == 0){
            p_sMessage = "No incentive added.";
            return false;
        }
        
        p_oAllctn.beforeFirst();
        while (p_oAllctn.next()){            
            if (DecryptAmount(p_oAllctn.getString("nInctvAmt")) <= 0.00){
                p_sMessage = "Invalid incentive amount for " + p_oAllctn.getString("xInctvNme");
                return false;
            }    
        }
        
        //validate employee incentive allocation
        if (getIncentiveEmployeeAllocationCount() == 0){
            p_sMessage = "No incentive allocation for employees.";
            return false;
        }
        
        p_oAllctn_Emp.beforeFirst();
        while (p_oAllctn_Emp.next()){
            if (p_oAllctn_Emp.getDouble("nAllcPerc") < 0.00){
                p_sMessage = p_oAllctn_Emp.getString("xEmployNm") + " has negative incentive percentage allocation.";
                return false;
            }
            
            if (DecryptAmount(p_oAllctn_Emp.getString("nAllcAmtx")) < 0.00){
                p_sMessage = p_oAllctn_Emp.getString("xEmployNm") + " has negative incentive amount allocation.";
                return false;
            }
        }
        
        //validate deductions
        if (getDeductionCount() > 0){
            //validate employee incentive allocation
            if (getDeductionEmployeeAllocationCount()== 0){
                p_sMessage = "No deduction allocation for employees.";
                return false;
            }
            
            p_oDedctn_Emp.beforeFirst();
            while (p_oDedctn_Emp.next()){
                if (p_oDedctn_Emp.getDouble("nAllcPerc") < 0.00){
                    p_sMessage = p_oDedctn_Emp.getString("xEmployNm") + " has negative deduction percentage allocation.";
                    return false;
                }

                if (DecryptAmount(p_oDedctn_Emp.getString("nAllcAmtx")) < 0.00){
                    p_sMessage = p_oDedctn_Emp.getString("xEmployNm") + " has negative deduction amount allocation.";
                    return false;
                }
            }
        }
        
        return true;
    }
    
    
    public String getSQ_Master(){
        String lsSQL = "";
        String lsStat = String.valueOf(p_nTranStat);
//        String lsCondition = "";
//        
//        if (lsStat.length() > 1){
//            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
//                lsSQL += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
//            }
//            
//            lsSQL = " AND a.cTranStat IN (" + lsSQL.substring(2) + ")";
//        } else{            
//            lsSQL = " AND a.cTranStat = " + SQLUtil.toSQL(lsStat);
//        }
//                
        lsSQL = "SELECT" + 
                    "  a.sTransNox" +
                    ", a.dTransact" +
                    ", a.sDeptIDxx" +
                    ", a.sMonthxxx" +
                    ", a.sRemarksx" +
                    ", a.sPrepared" +
                    ", a.dPrepared" +
                    ", a.cApprovd1" +
                    ", a.sApprovd1" +
                    ", a.dApprovd1" +
                    ", a.cApprovd2" +
                    ", a.sApprovd2" +
                    ", a.dApprovd2" +
                    ", a.sBatchNox" +
                    ", a.cTranStat" +
                    ", c.sBranchNm xBranchNm" +
                    ", IFNULL(b.sDeptName, '') xDeptName" +
                " FROM Incentive_Master a" +
                        " LEFT JOIN Department b ON a.sDeptIDxx = b.sDeptIDxx" +
                    ", Branch c " +
                " WHERE LEFT(a.sTransNox, 4) = c.sBranchCd" +
                     lsCondition();
        
        return lsSQL;
    }
    
    private String getSQ_Detail(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", a.nEntryNox" +
                    ", a.sEmployID" +
                    ", a.nTotalAmt" +
                    ", IFNULL(c.sCompnyNm, '') xEmployNm" +
                    ", IFNULL(d.sEmpLevNm, '') xEmpLevNm" +
                    ", IFNULL(e.sPositnNm, '') xPositnNm" +
                    ", IFNULL(ROUND(DATEDIFF(NOW(), IFNULL(b.dStartEmp, b.dHiredxxx)) / 365), '') xSrvcYear" +
                    ", 0.00 xIncentve" +
                    ", 0.00 xDeductnx" +
                " FROM Incentive_Detail a" +
                    ", Employee_Master001 b" +
                        " LEFT JOIN Client_Master c ON b.sEmployID = c.sClientID" +
                        " LEFT JOIN Employee_Level d ON b.sEmpLevID = d.sEmpLevID" +
                        " LEFT JOIN `Position` e ON b.sPositnID = e.sPositnID" +
                " WHERE a.sEmployID = b.sEmployID" +
                " ORDER BY nEntryNox";
    }
    private String getSQ_Employee(){
        return "SELECT" +
                    "  IFNULL(a.sEmployID, '') sEmployID" +
                    ", IFNULL(b.sCompnyNm, '') xEmployNm" +
                    ", IFNULL(c.sEmpLevNm, '') xEmpLevNm" +
                    ", IFNULL(d.sPositnNm, '') xPositnNm" +
                    ", IFNULL(ROUND(DATEDIFF(NOW(), IFNULL(a.dStartEmp, a.dHiredxxx)) / 365), '') xSrvcYear" +
                " FROM Employee_Master001 a" +
                        " LEFT JOIN Client_Master b ON a.sEmployID = b.sClientID" +
                        " LEFT JOIN Employee_Level c ON a.sEmpLevID = c.sEmpLevID" +
                        " LEFT JOIN `Position` d ON a.sPositnID = d.sPositnID" +
                "  WHERE b.sClientID IS NOT NULL ";
    }
    
    private String getSQ_Detail_Allocation(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", a.sInctveCD" +
                    ", a.nQtyGoalx" +
                    ", a.nQtyActlx" +
                    ", a.nAmtGoalx" +
                    ", a.nAmtActlx" +
                    ", a.nInctvAmt" +
                    ", a.sRemarksx" +
                    ", IFNULL(b.sInctveDs, '') xInctvNme" +
                    ", IFNULL(b.cByPercnt, '') xByPercnt" +
                " FROM Incentive_Detail_Allocation a" +
                    ", Incentive b" +
                " WHERE a.sInctveCD = b.sInctveCD";
    }
    
    private String getSQ_Detail_Allocation_Emp(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", a.sEmployID" +
                    ", a.sInctveCD" +
                    ", a.nAllcPerc" +
                    ", a.nAllcAmtx" +
                    ", IFNULL(c.sInctveDs, '') xInctvNme" +
                    ", IFNULL(d.sCompnyNm, '') xEmployNm" +
                    ", IFNULL(e.sEmpLevNm, '') xEmpLevNm" +
                    ", IFNULL(f.sPositnNm, '') xPositnNm" +
                    ", IFNULL(ROUND(DATEDIFF(NOW(), IFNULL(b.dStartEmp, b.dHiredxxx)) / 365), '') xSrvcYear" +
                    ", 0.00 nTotalAmt" +
                " FROM Incentive_Detail_Allocation_Employee a" +
                        " LEFT JOIN Incentive c ON a.sInctveCD = c.sInctveCD" +
                    ", Employee_Master001 b" +
                        " LEFT JOIN Client_Master d ON b.sEmployID = d.sClientID" +
                        " LEFT JOIN Employee_Level e ON b.sEmpLevID = e.sEmpLevID" +
                        " LEFT JOIN `Position` f ON b.sPositnID = f.sPositnID" +
                " WHERE a.sEmployID = b.sEmployID" +
                " ORDER BY e.sEmpLevID DESC, IFNULL(b.dStartEmp, b.dHiredxxx), d.sCompnyNm";
    }
    
    private String getSQ_Detail_Deduction(){
        return "SELECT" +
                    "  sTransNox" +
                    ", nEntryNox" +
                    ", sRemarksx" +
                    ", nDedctAmt" +
                    ", '1' xNewValue" +
                " FROM Incentive_Detail_Ded_Allocation";
    }
    
    private String getSQ_Detail_Deduction_Emp(){
        return "SELECT" +
                    "  IFNULL(a.sTransNox, '') sTransNox" +
                    ", IFNULL(a.nEntryNox, '') nEntryNox" +
                    ", IFNULL(a.sEmployID, '') sEmployID" +
                    ", IFNULL(a.nAllcPerc, 0) nAllcPerc" +
                    ", IFNULL(a.nAllcAmtx, 0) nAllcAmtx" +
                    ", IFNULL(b.sCompnyNm, '') xEmployNm" +
                    ", '1' nNewValue" +
                    ", 0.00 nTotalAmt" +
                " FROM Incentive_Detail_Ded_Allocation_Employee a" +
                    " LEFT JOIN Client_Master b ON a.sEmployID = b.sClientID";
    }
    
    private String lsCondition(){
        String lsStat = String.valueOf(p_nTranStat);
        String lsCondition = "";
        if (lsStat.length() > 1){
            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
                lsCondition += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            
            lsCondition = " AND a.cTranStat IN (" + lsCondition.substring(2) + ")";
        } else{            
            lsCondition = " AND a.cTranStat = " + SQLUtil.toSQL(lsStat);
            
        }
        if (MAIN_OFFICE.contains(p_oApp.getBranchCode())){          
            System.out.println("department  = " + p_oApp.getDepartment());
            if ((AUDITOR + "»" + COLLECTION + "»" + FINANCE).contains(p_oApp.getDepartment())){
                if (p_oApp.getDepartment().equals(AUDITOR)){
                    lsCondition = lsCondition + " AND a.cApprovd2 = '0'";
                    System.out.println("lsCondition  = " + lsCondition);
                }
            }else{
                lsCondition = lsCondition + " AND LEFT(a.sTransNox,4) = " + SQLUtil.toSQL(p_oApp.getBranchCode());
            }
        }else{
           lsCondition = lsCondition + " AND LEFT(a.sTransNox,4)  = " + SQLUtil.toSQL(p_oApp.getBranchCode());

        }
        return lsCondition;
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
    
    private void loadConfig(){
        //update the value on configuration before deployment
        System.setProperty(DEBUG_MODE, "0"); 
        System.setProperty(REQUIRE_CSS, "0");
        System.setProperty(REQUIRE_CM, "1");
        System.setProperty(REQUIRE_BANK_ON_APPROVAL, "0");
    }
}
