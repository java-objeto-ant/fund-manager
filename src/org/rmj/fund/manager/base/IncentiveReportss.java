/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.fund.manager.base;

import com.sun.rowset.CachedRowSetImpl;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.constants.UserRight;
import org.rmj.fund.manager.parameters.IncentiveBankInfo;

/**
 *
 * @author User
 */
public class IncentiveReportss {
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

    private CachedRowSet p_oBranch;
    private CachedRowSet p_oMaster;
    private CachedRowSet p_oDetail;
    private CachedRowSet p_oDetailVal;
    private CachedRowSet p_oDetailEmp;
    private CachedRowSet p_oIncCateg;
    private CachedRowSet p_oDetailCateg;
    private CachedRowSet p_oAllctn;
    private CachedRowSet p_oAllctn_Emp;
    private CachedRowSet p_oDedctn;
    private CachedRowSet p_oDedctn_Emp;
    private CachedRowSet p_oEmployee;
    
    private LMasDetTrans p_oListener;
   
    public IncentiveReportss(GRider foApp, String fsBranchCd, boolean fbWithParent){        
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
   
    public boolean OpenTransactionEmployee(String fsValue) throws SQLException{
         
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        createDetailEmployee();
        createDetailEmployeeNew();
        p_sMessage = ""; 
        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        String lsCondition ="";
        String lsCondition1 ="";
         
        if(p_oEmployee != null){
//            lsCondition = lsCondition + " AND a.sEmployID = " +  SQLUtil.toSQL(getEmployee("sEmployID"));  
            lsCondition = " AND a.sTransNox LIKE " + SQLUtil.toSQL(getBranch("sBranchCd") + "%");
            lsCondition1 = " AND a.sTransNox LIKE " + SQLUtil.toSQL(getBranch("sBranchCd") + "%");
        }
        
//        if(p_oBranch != null){
////            lsCondition = lsCondition + " AND f.sBranchCD = " +  SQLUtil.toSQL(getBranch("sBranchCd"));
//            lsCondition =" AND  LEFT(a.sTransNox, 4) = LEFT(b.sEmployID, 4)";
//            lsCondition1 =" AND  LEFT(a.sTransNox, 4) = LEFT(b.sEmployID, 4)";
//        }
        
        if(!fsValue.isEmpty()){
           lsCondition =  lsCondition + " AND a.sMonthxxx = " +SQLUtil.toSQL(fsValue);
           lsCondition1 =  lsCondition1 + " AND a.sMonthxxx = " +SQLUtil.toSQL(fsValue);
        }
        
        lsCondition =  lsCondition + " GROUP BY b.sEmployID,a.sMonthxxx,a.sTransNox";
        lsSQL = getSQ_Detail() + lsCondition ;
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDetailEmp = factory.createCachedRowSet();
        p_oDetailEmp.populate(loRS);
        MiscUtil.close(loRS);
        computeEmpTotalIncentiveAmount();
        
        lsCondition1 =  lsCondition1 + " GROUP BY b.sEmployID,a.sMonthxxx";
        lsSQL = getSQ_Detail() + lsCondition1 ;
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDetailVal = factory.createCachedRowSet();
        p_oDetailVal.populate(loRS);
        computeEmpTotalIncentiveAmountNew();
        MiscUtil.close(loRS);
        p_nEditMode = EditMode.READY;
        return true;
    }
    
    private void computeEmpTotalIncentiveAmountNew() throws SQLException{
        int lnDetRow = getNewEmpCount();
        int lnIncRow;
        int lnAlcRow;
        
        int lnCtr1, lnCtr2, lnCtr3;
        double lnTotalAmt, lnPercentx, lnAllcAmtx, lnIncentve, lnDeductnx;
        double lnTotalInc, lnTotalDed;     
        double transTotal = 0.0;      
        
        for (lnCtr1 = 1; lnCtr1 <= lnDetRow; lnCtr1++){
            p_oDetailVal.absolute(lnCtr1);
            
            lnTotalInc = 0.00;
            lnTotalDed = 0.00;
            lnTotalAmt = 0.00;
            String lsSQL;
            String lsCondition;
            ResultSet loRS;
            RowSetFactory factory = RowSetProvider.newFactory();
            
             for (lnCtr2 = 1; lnCtr2 <= getEmpCount(); lnCtr2++){
                 
                 p_oDetailEmp.absolute(lnCtr2);
//                 p_oAllctn.getString("sInctveCD").equals(p_oAllctn_Emp.getString("sInctveCD")
                 if(p_oDetailEmp.getString("sEmployID").equals(p_oDetailVal.getString("sEmployID"))){
                     lnTotalInc += Double.parseDouble(p_oDetailEmp.getString("xIncentve"));
                     lnTotalDed += Double.parseDouble(p_oDetailEmp.getString("xDeductnx"));
                     lnTotalAmt += DecryptAmount(p_oDetailEmp.getString("nTotalAmt"));
                 }
             }
            
            p_oDetailVal.updateString("xIncentve", String.valueOf(lnTotalInc));
            p_oDetailVal.updateString("xDeductnx", String.valueOf(lnTotalDed));
            p_oDetailVal.updateString("nTotalAmt", EncryptAmount(lnTotalAmt));
            transTotal = transTotal + (lnTotalInc - lnTotalDed);
            p_oDetailVal.updateRow();
            
         
        }
        
    }
    public boolean OpenTransactionCategory(String fsValue) throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";
        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        String lsCondition = "";
         
        if(!fsValue.isEmpty()){
           lsCondition =  lsCondition + " AND a.sMonthxxx = " +SQLUtil.toSQL(fsValue);
        }

        lsSQL = getSQ_EmployeeDetail() + lsCondition;
        loRS = p_oApp.executeQuery(lsSQL);
        
        if (MiscUtil.RecordCount(loRS) <= 0){
            p_sMessage = "No record to load.";
            return false;
        }
        
        p_oDetailCateg = factory.createCachedRowSet();
        p_oDetailCateg.populate(loRS);
        MiscUtil.close(loRS);
        
        createIncByCategory();
        double lnValue = 0.00;
        
        for (int lnCtr = 1; lnCtr <= getCategoryCount(); lnCtr++){
            lsSQL = (String) getDetailCategory(lnCtr, "sEmployID");
            
            setIncByCategory(lsSQL, "sEmployID", (String) getDetailCategory(lnCtr, "sEmployID"));
            setIncByCategory(lsSQL, "xBranchNm", (String) getDetailCategory(lnCtr, "xBranchNm"));
            setIncByCategory(lsSQL, "xEmployNm", (String) getDetailCategory(lnCtr, "xEmployNm"));
            setIncByCategory(lsSQL, "xPositnNm", (String) getDetailCategory(lnCtr, "xPositnNm"));
            setIncByCategory(lsSQL, "sMonthxxx", (String) getDetailCategory(lnCtr, "sMonthxxx"));
            
            lnValue = ((double) getDetailCategory(lnCtr, "xMCSalesx") * (double) getDetailCategory(lnCtr, "pMCSalesx") / 100) + (double) getDetailCategory(lnCtr, "aMCSalesx");
            lnValue += (double) getIncByEmployee(lsSQL, "xMCSalesx");
            setIncByCategory(lsSQL, "xMCSalesx", lnValue);
                 
            lnValue = ((double) getDetailCategory(lnCtr, "xSPSalesx") * (double) getDetailCategory(lnCtr, "pSPSalesx") / 100) + (double) getDetailCategory(lnCtr, "aSPSalesx");
            lnValue += (double) getIncByEmployee(lsSQL, "xSPSalesx");
            setIncByCategory(lsSQL, "xSPSalesx", lnValue);
            
            lnValue = ((double) getDetailCategory(lnCtr, "xServicex") * (double) getDetailCategory(lnCtr, "pServicex") / 100) + (double) getDetailCategory(lnCtr, "aServicex");
            lnValue += (double) getIncByEmployee(lsSQL, "xServicex");
            setIncByCategory(lsSQL, "xServicex", lnValue);
            
            lnValue = ((double) getDetailCategory(lnCtr, "xLTOPoolx") * (double) getDetailCategory(lnCtr, "pLTOPoolx") / 100) + (double) getDetailCategory(lnCtr, "aLTOPoolx");
            lnValue += (double) getIncByEmployee(lsSQL, "xLTOPoolx");
            setIncByCategory(lsSQL, "xLTOPoolx", lnValue);
            
            lnValue = ((double) getDetailCategory(lnCtr, "xDEI2xxxx") * (double) getDetailCategory(lnCtr, "pDEI2xxxx") / 100) + (double) getDetailCategory(lnCtr, "aDEI2xxxx");
            lnValue += (double) getIncByEmployee(lsSQL, "xDEI2xxxx");
            setIncByCategory(lsSQL, "xDEI2xxxx", lnValue);
            
            lnValue = ((double) getDetailCategory(lnCtr, "xCollectn") * (double) getDetailCategory(lnCtr, "pCollectn") / 100) + (double) getDetailCategory(lnCtr, "aCollectn");
            lnValue += (double) getIncByEmployee(lsSQL, "xDEI2xxxx");
            setIncByCategory(lsSQL, "xDEI2xxxx", lnValue);
            
            lsSQL = "SELECT" +
                            "  a.sEmployID" +
                            ", b.sRemarksx" +
                            ", b.nDedctAmt" +
                            ", a.nAllcAmtx" +
                            ", a.nAllcPerc" +
                    " FROM Incentive_Detail_Ded_Allocation_Employee a" +
                            ", Incentive_Detail_Ded_Allocation b" +
                    " WHERE a.sTransNox = b.sTransNox";
            
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox = " + SQLUtil.toSQL((String) getDetailCategory(lnCtr, "sTransNox")) +
                                                    " AND a.sEmployID = " + SQLUtil.toSQL((String) getDetailCategory(lnCtr, "sEmployID")));
            
            loRS = p_oApp.executeQuery(lsSQL);
            lnValue = 0.00;
            while (loRS.next()){
                try {
                    lnValue = Double.valueOf(loRS.getString("nAllcPerc"));
                } catch (NumberFormatException e) {
                    lnValue =  DecryptAmount(loRS.getString("nAllcPerc"));
                } 
                
                lnValue = (DecryptAmount(loRS.getString("nDedctAmt")) * lnValue / 100) + DecryptAmount(loRS.getString("nAllcAmtx"));
                lnValue += (double) getIncByEmployee(loRS.getString("sEmployID"), "xDeductnx");
                setIncByCategory(loRS.getString("sEmployID"), "xDeductnx", lnValue);
            }  
            MiscUtil.close(loRS);
        }

        p_nEditMode = EditMode.READY;
        return true;
    }
    
    private void computeEmpTotalIncentivesAmounts() throws SQLException{
        int lnIncRow;
        int lnAlcRow;
        
        
        double lnPercentx,  lnIncentve,lnAllcAmtx,lnTotalAmt,lnTotalDed,lnDeductnx; 
                        
        String[] arrXInctvPrc;
        String[] arrXInctvAmt;
        String[] arrXIncentve;
        String[] arrSInctveCD;
        double lnInctvAmt;
        int lnCtr,lnCtr2,lnCtr3;
        
         for (lnCtr = 1; lnCtr <= getCategoryCount(); lnCtr++){
            p_oDetailCateg.absolute(lnCtr);
            lnIncentve = 0.0;
            lnPercentx = 0.0;
            lnInctvAmt = 0.00;
            String lsSQL;
            String lsCondition;
            ResultSet loRS;
            RowSetFactory factory = RowSetProvider.newFactory();
            createDetailAllocation();
            createDetailAllocationEmp();
            createDetailDeductionAlloc();
            createDetailDeductionAllocEmp();
            
            //open deductions
            lsSQL = MiscUtil.addCondition(getSQ_Detail_Deduction(), "sTransNox = " + SQLUtil.toSQL(p_oDetailCateg.getString("sTransNox")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oDedctn = factory.createCachedRowSet();
            p_oDedctn.populate(loRS);
            MiscUtil.close(loRS);

            //open deductions employee alloction
            lsSQL = MiscUtil.addCondition(getSQ_Detail_Deduction_Emp(), "a.sTransNox = " + SQLUtil.toSQL(p_oDetailCateg.getString("sTransNox")) + "AND a.sEmployID = " + SQLUtil.toSQL(p_oDetailCateg.getString("sEmployID")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oDedctn_Emp = factory.createCachedRowSet();
            p_oDedctn_Emp.populate(loRS);
            MiscUtil.close(loRS);
            
            arrSInctveCD = p_oDetailCateg.getString("sInctveCD").split("/");
            for(int lnctr = 0; lnctr < arrSInctveCD.length; lnctr++){
                arrXInctvPrc = p_oDetailCateg.getString("xInctvPrc").split("/");
                arrXInctvAmt = p_oDetailCateg.getString("xInctvAmt").split("/");
                arrXIncentve = p_oDetailCateg.getString("xIncentve").split("/");
                
                if (Double.parseDouble(arrXInctvPrc[lnctr]) > 0.00){
                    lnInctvAmt = DecryptAmount(arrXInctvAmt[lnctr]);
                    lnInctvAmt = lnInctvAmt * Double.parseDouble(arrXInctvPrc[lnctr]) / 100;

                }else{
                    lnInctvAmt = DecryptAmount(arrXIncentve[lnctr]);
                }
                if(arrSInctveCD[lnctr].equalsIgnoreCase("001")){
                    p_oDetailCateg.updateString("nMcSalesx", EncryptAmount(lnInctvAmt)); 
                }else if(arrSInctveCD[lnctr].equalsIgnoreCase("002")){
                    p_oDetailCateg.updateString("nSpareprt", EncryptAmount(lnInctvAmt));
                }else if(arrSInctveCD[lnctr].equalsIgnoreCase("003")){
                    p_oDetailCateg.updateString("nServicex", EncryptAmount(lnInctvAmt));
                }else if(arrSInctveCD[lnctr].equalsIgnoreCase("004")){
                    p_oDetailCateg.updateString("nRegisTri", EncryptAmount(lnInctvAmt));
                }else if(arrSInctveCD[lnctr].equalsIgnoreCase("005")){
                    p_oDetailCateg.updateString("nDei2xxxx", EncryptAmount(lnInctvAmt));
                }
                p_oDetailCateg.updateRow();
                
            }
          //deductions
          
            lnAllcAmtx = 0.0;
            lnTotalDed = 0.0;
            lnTotalAmt = 0.00;
            lnIncRow = getDeductionCount();
            lnAlcRow = getDeductionEmployeeAllocationCount();
            for (lnCtr2 = 1; lnCtr2 <= lnIncRow; lnCtr2++){
                p_oDedctn.absolute(lnCtr2);
                
                lnAllcAmtx = getAllocatedDeduction(lnCtr2, "2");
                
                for (lnCtr3 = 1; lnCtr3 <= lnAlcRow; lnCtr3++){
                    p_oDedctn_Emp.absolute(lnCtr3);
                    
                    lnDeductnx = 0.00;
                    if (p_oDedctn.getInt("nEntryNox") == p_oDedctn_Emp.getInt("nEntryNox") &&
                        p_oDetailCateg.getString("sEmployID").equals(p_oDedctn_Emp.getString("sEmployID"))){
                        
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
            
            p_oDetailCateg.updateString("xDeductnx", String.valueOf(lnTotalDed));
            p_oDetailCateg.updateRow();
           
           
        
         }
    }
//    
    public boolean OpenTransactionMaster(String fsValue) throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        createDetail();
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
        
        String lsCondition = "";
        String lsCondition2 = "";
        if(p_oBranch != null){
            lsCondition = lsCondition + " AND c.sBranchCD = " +  SQLUtil.toSQL(getBranch("sBranchCd"));
        }
//        else{
//            lsCondition = lsCondition + " AND c.sBranchCD = " +  SQLUtil.toSQL(p_oApp.getBranchCode());
//        }
        
        if(!fsValue.isEmpty()){
           lsCondition =  lsCondition + " AND a.sMonthxxx = " +SQLUtil.toSQL(fsValue);
        }
        lsSQL = getSQ_Master() + lsCondition + " ORDER BY a.sTransNox, a.sMonthxxx";
        System.out.println("master = " + lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oMaster = factory.createCachedRowSet();
        p_oMaster.populate(loRS);
        MiscUtil.close(loRS);
        
//        lsSQL = getSQ_MasterVal()+ lsCondition + " ORDER BY xBranchNm, a.sMonthxxx";
//        System.out.println("master = " + lsSQL);
//        loRS = p_oApp.executeQuery(lsSQL);
//        p_oMasterVal = factory.createCachedRowSet();
//        p_oMasterVal.populate(loRS);
//        MiscUtil.close(loRS);
//        computeEmpTotalIncentiveAmountMaster() ;
        computeSmmaryIncentiveAmountMaster();
        p_nEditMode = EditMode.READY;
        return true;
    }
    private void computeSmmaryIncentiveAmountMaster() throws SQLException{
        int lnCtr4;    
        for(lnCtr4 = 1; lnCtr4 <= getItemMasterCount(); lnCtr4++){
            
            p_oMaster.absolute(lnCtr4);
             
            String lsSQL;
            ResultSet loRS;
            RowSetFactory factory = RowSetProvider.newFactory();
        
        
//        
//            //open detail
            lsSQL = MiscUtil.addCondition(getSQ_MasterDetail(), "a.sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oDetail = factory.createCachedRowSet();
            p_oDetail.populate(loRS);
            MiscUtil.close(loRS);
            //open incentive
            lsSQL = MiscUtil.addCondition(getSQ_Detail_Allocation(), "a.sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oAllctn = factory.createCachedRowSet();
            p_oAllctn.populate(loRS);
            MiscUtil.close(loRS);

            //open incentive employee allocation
            lsSQL = MiscUtil.addCondition(getSQ_Detail_Allocation_Emp(), "a.sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oAllctn_Emp = factory.createCachedRowSet();
            p_oAllctn_Emp.populate(loRS);
            MiscUtil.close(loRS);

            //open deductions
            lsSQL = MiscUtil.addCondition(getSQ_Detail_Deduction(), "sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oDedctn = factory.createCachedRowSet();
            p_oDedctn.populate(loRS);
            MiscUtil.close(loRS);

            //open deductions employee alloction
            lsSQL = MiscUtil.addCondition(getSQ_Detail_Deduction_Emp(), "a.sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oDedctn_Emp = factory.createCachedRowSet();
            p_oDedctn_Emp.populate(loRS);
            MiscUtil.close(loRS);
            int lnCtr1, lnCtr2, lnCtr3;
            int lnDetRow = getItemCount();
            int lnIncRow;
            int lnAlcRow;
            double lnTotalAmt, lnPercentx, lnAllcAmtx, lnIncentve, lnDeductnx;
            double lnMCSalesx, lnSpareprt, lnServicex, lnRegistri, lnDEIxxxxx,lnXDeductnx;
            double lnTotalInc, lnTotalDed;     
            double transTotal = 0.0;  
            double lnMCSalesTotal = 0.0;  
            double lnSpareprtTotal = 0.0;  
            double lnServicexTotal = 0.0;  
            double lnRegistriTotal = 0.0;  
            double lnDEIxxxxxTotal = 0.0;  
            double lnXDeductnxTotal = 0.0;  
            
            for (lnCtr1 = 1; lnCtr1 <= lnDetRow; lnCtr1++){
                p_oDetail.absolute(lnCtr1);
                lnTotalInc = 0.00;
                lnTotalDed = 0.00;
                lnTotalAmt = 0.00;
                lnMCSalesx = 0.00;
                lnSpareprt = 0.00;
                lnServicex = 0.00;
                lnRegistri = 0.00; 
                lnDEIxxxxx  = 0.00;

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
                            getDetail(lnCtr1,"sEmployID").equals(p_oAllctn_Emp.getString("sEmployID"))){

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
                            if(p_oAllctn_Emp.getString("sInctveCD").equalsIgnoreCase("001")){
                                 lnMCSalesx += lnIncentve; 
                                
                            }else if(p_oAllctn_Emp.getString("sInctveCD").equalsIgnoreCase("002")){
                                lnSpareprt += lnIncentve; 
                                
                            }else if(p_oAllctn_Emp.getString("sInctveCD").equalsIgnoreCase("003")){
                                lnServicex += lnIncentve; 
                            }else if(p_oAllctn_Emp.getString("sInctveCD").equalsIgnoreCase("004")){
                                lnRegistri += lnIncentve; 
                            }else if(p_oAllctn_Emp.getString("sInctveCD").equalsIgnoreCase("005")){
                                lnDEIxxxxx += lnIncentve;
                                
                            }
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
            if(p_oAllctn_Emp.getString("sInctveCD").equalsIgnoreCase("001")){
                p_oDetail.updateDouble("nMcSalesx", lnMCSalesx);
           }else if(p_oAllctn_Emp.getString("sInctveCD").equalsIgnoreCase("002")){
                p_oDetail.updateDouble("nSpareprt", lnSpareprt);
           }else if(p_oAllctn_Emp.getString("sInctveCD").equalsIgnoreCase("003")){
                p_oDetail.updateDouble("nServicex", lnServicex);
           }else if(p_oAllctn_Emp.getString("sInctveCD").equalsIgnoreCase("004")){
                p_oDetail.updateDouble("nRegisTri", lnRegistri);
           }else if(p_oAllctn_Emp.getString("sInctveCD").equalsIgnoreCase("005")){
                p_oDetail.updateDouble("nDei2xxxx", lnDEIxxxxx);
           }
            p_oDetail.updateDouble("xIncentve", lnTotalInc);
            p_oDetail.updateDouble("xDeductnx", lnTotalDed);
            p_oDetail.updateString("nTotalAmt", EncryptAmount(lnTotalInc - lnTotalDed));
            transTotal = transTotal + (lnTotalInc - lnTotalDed);
            
            
            lnMCSalesTotal = lnMCSalesTotal + lnMCSalesx;  
            lnSpareprtTotal = lnSpareprtTotal + lnSpareprt;  
            lnServicexTotal = lnServicexTotal + lnServicex;  
            lnRegistriTotal = lnRegistriTotal + lnRegistri;  
            lnDEIxxxxxTotal = lnDEIxxxxxTotal + lnDEIxxxxx;  
            lnXDeductnxTotal = lnXDeductnxTotal + lnTotalDed;
            
          
            }
            p_oMaster.updateString("xTotalAmt", String.valueOf(transTotal));
            p_oMaster.updateString("nMcSalesx", String.valueOf(lnMCSalesTotal));
            p_oMaster.updateString("nSpareprt", String.valueOf(lnSpareprtTotal));
            p_oMaster.updateString("nServicex", String.valueOf(lnServicexTotal));
            p_oMaster.updateString("nRegisTri", String.valueOf(lnRegistriTotal));
            p_oMaster.updateString("nDei2xxxx", String.valueOf(lnDEIxxxxxTotal));
            p_oMaster.updateString("xDeductnx", String.valueOf(lnXDeductnxTotal));
            p_oMaster.updateRow();
        }
        
        
    }
    public Double OpenToTalMaster(int fnRow,String fsTransNox) throws SQLException{
        p_nEditMode = EditMode.UNKNOWN;
        
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return 0.0;
        }
        
        p_sMessage = "";
        
        if (System.getProperty(DEBUG_MODE).equals("0")){
            if (Integer.valueOf(p_oApp.getEmployeeLevel()) < 1){
                p_sMessage = "Your employee level is not authorized to use this transaction.";
                return 0.0;
            }

            if (p_oApp.getUserLevel() < UserRight.SUPERVISOR){
                p_sMessage = "Your account level is not authorized to use this transaction.";
                return 0.0;
            }
        }  
        
        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        
        
        
        //open detail
        lsSQL = MiscUtil.addCondition(getSQ_MasterDetail(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
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
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDedctn_Emp = factory.createCachedRowSet();
        p_oDedctn_Emp.populate(loRS);
        MiscUtil.close(loRS);
        
        computeEmpTotalIncentiveAmount();
        double transTotal = 0;
        for(int x = 1; x <= getItemCount(); x++){
            transTotal = transTotal + Double.parseDouble(getDetail(x, "nTotalAmt").toString());
        }
        
        
        return transTotal;
    }
    
    public boolean OpenBranch(String fsBranch) throws SQLException{
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
        lsSQL = MiscUtil.addCondition(getSQ_Branch(), "sBranchCd = " + SQLUtil.toSQL(fsBranch));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oBranch = factory.createCachedRowSet();
        p_oBranch.populate(loRS);
        MiscUtil.close(loRS);
        
        p_nEditMode = EditMode.READY;
        
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
    public int getEmpCount() throws SQLException{
        p_oDetailEmp.last();
        return p_oDetailEmp.getRow();
    }public int getNewEmpCount() throws SQLException{
        p_oDetailVal.last();
        return p_oDetailVal.getRow();
    }
    
    public int getCategoryCount() throws SQLException{
        p_oDetailCateg.last();
        return p_oDetailCateg.getRow();
    }
    
    public int getIncByCategoryCount() throws SQLException{
        p_oIncCateg.last();
        return p_oIncCateg.getRow();
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
    
    public void setDetailCategory(int fnRow, int fnIndex, Object foValue) throws SQLException{
        if (getCategoryCount()== 0 || fnRow == 0) return;
        
        p_oDetailCateg.absolute(fnRow);
    
        p_oDetailCateg.updateObject(fnIndex, foValue);
    }
    
    public void setDetailCategory(int fnRow, String fsIndex, Object foValue) throws SQLException{
        setDetailCategory(fnRow, getColumnIndex(p_oDetailCateg, fsIndex), foValue);
    }
    
    public void setIncByCategory(String fsEmployID, String fsIndex, Object foValue) throws SQLException{
        setIncByCategory(fsEmployID, getColumnIndex(p_oIncCateg, fsIndex), foValue);
    }
    
    public void setIncByCategory(String fsEmployID, int fnIndex, Object foValue) throws SQLException{
        if (fsEmployID.isEmpty()) return;
        
        if (getIncByCategoryCount() == 0){
            p_oIncCateg.moveToInsertRow();
            MiscUtil.initRowSet(p_oIncCateg);       
            p_oIncCateg.updateObject(fnIndex, foValue);
            p_oIncCateg.insertRow();
            p_oIncCateg.moveToCurrentRow();
        } else {
            p_oIncCateg.beforeFirst();
            
            boolean lbAdded = false;
            while(p_oIncCateg.next()){
                if (fsEmployID.equals(p_oIncCateg.getString("sEmployID"))){
                    p_oIncCateg.updateObject(fnIndex, foValue);
                    p_oIncCateg.updateRow();
                    lbAdded = true;
                }
            }
            
            if (!lbAdded){
                p_oIncCateg.last();
                p_oIncCateg.moveToInsertRow();
                MiscUtil.initRowSet(p_oIncCateg);       
                p_oIncCateg.updateObject(fnIndex, foValue);
                p_oIncCateg.insertRow();
                p_oIncCateg.moveToCurrentRow();
            }
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
    
    public Object getDetailEmployee(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getEmpCount()== 0 || fnRow > getEmpCount()) return null;
        
        p_oDetailEmp.absolute(fnRow);
        switch (fnIndex){
            case 6://nTotalAmt
                return DecryptAmount(p_oDetailEmp.getString(fnIndex));
            default:
                return p_oDetailEmp.getObject(fnIndex);
        }
    }
    public Object getDetailEmployee(int fnRow, String fsIndex) throws SQLException{
        return getDetailEmployee(fnRow, getColumnIndex(p_oDetailEmp, fsIndex));
    }
    
    public Object getNewDetailEmployee(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getNewEmpCount()== 0 || fnRow > getNewEmpCount()) return null;
        
        p_oDetailVal.absolute(fnRow);
        switch (fnIndex){
            case 6://nTotalAmt
                return DecryptAmount(p_oDetailVal.getString(fnIndex));
            default:
                return p_oDetailVal.getObject(fnIndex);
        }
    }
    public Object getNewDetailEmployee(int fnRow, String fsIndex) throws SQLException{
        return getNewDetailEmployee(fnRow, getColumnIndex(p_oDetailVal, fsIndex));
    }
    
    public Object getDetailCategory(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getCategoryCount() == 0 || fnRow > getCategoryCount()) return null;

        p_oDetailCateg.absolute(fnRow);
        switch (fnIndex){
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
                try {
                    return Double.valueOf(p_oDetailCateg.getString(fnIndex));
                } catch (NumberFormatException e) {
                    return DecryptAmount(p_oDetailCateg.getString(fnIndex));
                }
            default:
                return p_oDetailCateg.getObject(fnIndex);
        }
    }
    
    public Object getDetailCategory(int fnRow, String fsIndex) throws SQLException{
        return getDetailCategory(fnRow,getColumnIndex(p_oDetailCateg, fsIndex));
    }
    
    public Object getIncByCategory(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getIncByCategoryCount() == 0 || getIncByCategoryCount() < fnRow) return null;

        p_oIncCateg.absolute(fnRow);
        return p_oIncCateg.getObject(fnIndex);
    }
    
    public Object getIncByCategory(int fnRow, String fsIndex) throws SQLException{
        return getIncByCategory(fnRow,getColumnIndex(p_oIncCateg, fsIndex));
    }
    
    private Object getIncByEmployee(String fsEmployID, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getIncByCategoryCount() == 0) return null;

        p_oIncCateg.beforeFirst();
        while(p_oIncCateg.next()){
            if (fsEmployID.equals(p_oIncCateg.getString("sEmployID"))){
                return p_oIncCateg.getObject(fnIndex);
            }
        }
        return null;
    }
    
    private Object getIncByEmployee(String fsEmployID, String fsIndex) throws SQLException{
        return getIncByEmployee(fsEmployID,getColumnIndex(p_oIncCateg, fsIndex));
    }
    
    public int getItemMasterCount() throws SQLException{
        p_oMaster.last();
        return p_oMaster.getRow();
    }
    
    public Object getMaster(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getItemMasterCount() == 0 || fnRow > getItemMasterCount()) return null;
        
        p_oMaster.absolute(fnRow);
        return p_oMaster.getObject(fnIndex);
    }
    
    public Object getMaster(int fnRow, String fsIndex) throws SQLException{
        return getMaster(fnRow, getColumnIndex(p_oMaster, fsIndex));
    }
    
    public void setMaster(int fnRow,int fnIndex, Object foValue) throws SQLException{
        switch (fnIndex){
            case 3://sDeptIDxx
                searchDepartment(fnRow,(String) foValue, true);
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
    public Object getBranch(int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oBranch.first();
        return p_oBranch.getObject(fnIndex);
    }
    
    public Object getBranch(String fsIndex) throws SQLException{
        return getBranch(getColumnIndex(p_oBranch, fsIndex));
    }
    public void setBranch(){
        p_oBranch = null;
    }
    
    
    public Object getEmployee(int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oEmployee.first();
        return p_oEmployee.getObject(fnIndex);
    }
    
    public Object getEmployee(String fsIndex) throws SQLException{
        return getEmployee(getColumnIndex(p_oEmployee, fsIndex));
    }
    
    
    public void setEmployee(){
        p_oEmployee = null;
    }
    
    
    public boolean searchDepartment(int fnRow,String fsValue, boolean fbByCode) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW) return false;
        
        if (fbByCode)
            if (fsValue.equals((String) getMaster(fnRow,"sDeptIDxx"))) return true;
        else
            if (fsValue.equals((String) getMaster(fnRow,"xDeptName"))) return true;
        
            
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
                createDetailEmployee(); 
                createDetailAllocation();
                createDetailAllocationEmp();
                createDetailDeductionAlloc();
                createDetailDeductionAllocEmp();

                if (p_oListener != null) p_oListener.MasterRetreive(17, getMaster(fnRow,"xDeptName"));
                
                return true;
            }
            
            p_oMaster.updateString("sDeptIDxx", "");
            p_oMaster.updateString("xDeptName", "");
            p_oMaster.updateRow();
                        
            //recreate detail and other tables
            createDetail();
            createDetailEmployee();
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
        createDetailEmployee();
        createDetailAllocation();
        createDetailAllocationEmp();
        createDetailDeductionAlloc();
        createDetailDeductionAllocEmp();
        
        return true;
    }
    public boolean searchBranch(String fsValue, boolean fbByCode) throws SQLException{
      
        
//        if (fbByCode)
//            if (fsValue.equals((String) getBranch("sBranchCd"))) return true;
//        else
//            if (fsValue.equals((String) getBranch("sBranchNm"))) return true;
        createBranch();
        String lsSQL = getSQ_Branch();
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchCd = " + SQLUtil.toSQL(fsValue));
        else
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
        
      
        JSONObject loJSON;
        
        if (p_bWithUI){
            loJSON = showFXDialog.jsonSearch(
                        p_oApp, 
                        lsSQL, 
                        fsValue, 
                        "Code»Branch Name", 
                        "sBranchCd»sBranchNm", 
                        "sBranchCd»sBranchNm", 
                        fbByCode ? 0 : 1);
            
            if (loJSON != null) 
                return OpenBranch((String) loJSON.get("sBranchCd"));
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchCd = " + SQLUtil.toSQL(fsValue));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%")); 
            lsSQL += " LIMIT 1";
        }
        
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        if (!loRS.next()){
            MiscUtil.close(loRS);
            p_sMessage = "No bracnh found for the givern criteria.";
            return false;
        }
        
        lsSQL = loRS.getString("sBranchCd");
        MiscUtil.close(loRS);
        
        return OpenBranch(lsSQL);
    }
   
    public String getMessage(){
        return p_sMessage;
    }
    
    private void computeEmpTotalIncentiveAmountMaster() throws SQLException{
        int lnCtr4;    
        for(lnCtr4 = 1; lnCtr4 <= getItemMasterCount(); lnCtr4++){
            
            p_oMaster.absolute(lnCtr4);
             
            String lsSQL;
            ResultSet loRS;
            RowSetFactory factory = RowSetProvider.newFactory();
        
        
        
            //open detail
            lsSQL = MiscUtil.addCondition(getSQ_MasterDetail(), "a.sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oDetail = factory.createCachedRowSet();
            p_oDetail.populate(loRS);
            MiscUtil.close(loRS);
            //open incentive
            lsSQL = MiscUtil.addCondition(getSQ_Detail_Allocation(), "a.sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oAllctn = factory.createCachedRowSet();
            p_oAllctn.populate(loRS);
            MiscUtil.close(loRS);

            //open incentive employee allocation
            lsSQL = MiscUtil.addCondition(getSQ_Detail_Allocation_Emp(), "a.sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oAllctn_Emp = factory.createCachedRowSet();
            p_oAllctn_Emp.populate(loRS);
            MiscUtil.close(loRS);

            //open deductions
            lsSQL = MiscUtil.addCondition(getSQ_Detail_Deduction(), "sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oDedctn = factory.createCachedRowSet();
            p_oDedctn.populate(loRS);
            MiscUtil.close(loRS);

            //open deductions employee alloction
            lsSQL = MiscUtil.addCondition(getSQ_Detail_Deduction_Emp(), "a.sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oDedctn_Emp = factory.createCachedRowSet();
            p_oDedctn_Emp.populate(loRS);
            MiscUtil.close(loRS);
            int lnCtr1, lnCtr2, lnCtr3;
            int lnDetRow = getItemCount();
            int lnIncRow;
            int lnAlcRow;
            double lnTotalAmt, lnPercentx, lnAllcAmtx, lnIncentve, lnDeductnx;
            double lnTotalInc, lnTotalDed;     
            double transTotal = 0.0;  
            
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
                            getDetail(lnCtr1,"sEmployID").equals(p_oAllctn_Emp.getString("sEmployID"))){

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
            transTotal = transTotal + (lnTotalInc - lnTotalDed);
            
          
            }
            p_oMaster.updateString("xTotalAmt", String.valueOf(transTotal));
            p_oMaster.updateRow();
        }
        
        
    }
    private void computeEmpTotalIncentiveAmount() throws SQLException{
        int lnDetRow = getEmpCount();
        int lnIncRow;
        int lnAlcRow;
        
        int lnCtr1, lnCtr2, lnCtr3;
        double lnTotalAmt, lnPercentx, lnAllcAmtx, lnIncentve, lnDeductnx;
        double lnTotalInc, lnTotalDed;     
        double transTotal = 0.0;       
        createDetailEmployeeNew();
        
        for (lnCtr1 = 1; lnCtr1 <= lnDetRow; lnCtr1++){
            p_oDetailEmp.absolute(lnCtr1);
            
            lnTotalInc = 0.00;
            lnTotalDed = 0.00;
            lnTotalAmt = 0.00;
            String lsSQL;
            String lsCondition;
            ResultSet loRS;
            RowSetFactory factory = RowSetProvider.newFactory();
            createDetailAllocation();
            createDetailAllocationEmp();
            createDetailDeductionAlloc();
            createDetailDeductionAllocEmp();
            
            
            //open incentive
            lsSQL = MiscUtil.addCondition(getSQ_Detail_Allocation(), "a.sTransNox = " + SQLUtil.toSQL(p_oDetailEmp.getString("sTransNox")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oAllctn = factory.createCachedRowSet();
            p_oAllctn.populate(loRS);
            MiscUtil.close(loRS);

            //open incentive employee allocation
            lsSQL = MiscUtil.addCondition(getSQ_Detail_Allocation_Emp(), "a.sTransNox = " + SQLUtil.toSQL(p_oDetailEmp.getString("sTransNox")) + " AND a.sEmployID = " + SQLUtil.toSQL(p_oDetailEmp.getString("sEmployID")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oAllctn_Emp = factory.createCachedRowSet();
            p_oAllctn_Emp.populate(loRS);
            MiscUtil.close(loRS);

            //open deductions
            lsSQL = MiscUtil.addCondition(getSQ_Detail_Deduction(), "sTransNox = " + SQLUtil.toSQL(p_oDetailEmp.getString("sTransNox")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oDedctn = factory.createCachedRowSet();
            p_oDedctn.populate(loRS);
            MiscUtil.close(loRS);

            //open deductions employee alloction
            lsSQL = MiscUtil.addCondition(getSQ_Detail_Deduction_Emp(), "a.sTransNox = " + SQLUtil.toSQL(p_oDetailEmp.getString("sTransNox")) + "AND a.sEmployID = " + SQLUtil.toSQL(p_oDetailEmp.getString("sEmployID")));
            loRS = p_oApp.executeQuery(lsSQL);
            p_oDedctn_Emp = factory.createCachedRowSet();
            p_oDedctn_Emp.populate(loRS);
            MiscUtil.close(loRS);
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
                         p_oDetailEmp.getString("sEmployID").equals(p_oAllctn_Emp.getString("sEmployID"))){
                        
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
                
                //check next record if same employee id
                //if same
                    //go to next, lnTotalInc + sa next
                //else
                    //go to next, reset lnTotalInc
                    //get inc value
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
                        p_oDetailEmp.getString("sEmployID").equals(p_oDedctn_Emp.getString("sEmployID"))){
                        
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
                
            p_oDetailEmp.updateString("xIncentve", String.valueOf(lnTotalInc));
            p_oDetailEmp.updateString("xDeductnx", String.valueOf(lnTotalDed));
            p_oDetailEmp.updateString("nTotalAmt", EncryptAmount(lnTotalInc - lnTotalDed));
            transTotal = transTotal + (lnTotalInc - lnTotalDed);
            p_oDetailEmp.updateRow();
            
         
        }
        
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
    
    
    private void createDetailDeductionAllocEmp() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();        

        meta.setColumnCount(7);
        
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
        
        meta.setColumnName(7, "nTotalAmt");
        meta.setColumnLabel(7, "nTotalAmt");
        meta.setColumnType(7, Types.DOUBLE);
        
        p_oDedctn_Emp = new CachedRowSetImpl();
        p_oDedctn_Emp.setMetaData(meta);        
    }
    
    private void createDetailDeductionAlloc() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();        

        meta.setColumnCount(4);
        
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
        
        p_oDedctn = new CachedRowSetImpl();
        p_oDedctn.setMetaData(meta);
    }
    
    private void createDetailAllocationEmp() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();        

        meta.setColumnCount(8);
        
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
        
        p_oAllctn_Emp = new CachedRowSetImpl();
        p_oAllctn_Emp.setMetaData(meta);
    }
    
    private void createIncByCategory() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();        

        meta.setColumnCount(12);
        
        meta.setColumnName(1, "sEmployID");
        meta.setColumnLabel(1, "sEmployID");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);
        
        meta.setColumnName(2, "xBranchNm");
        meta.setColumnLabel(2, "xBranchNm");
        meta.setColumnType(2, Types.VARCHAR);
        meta.setColumnDisplaySize(2, 50);
        
        meta.setColumnName(3, "xEmployNm");
        meta.setColumnLabel(3, "xEmployNm");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 128);
        
        meta.setColumnName(4, "xPositnNm");
        meta.setColumnLabel(4, "xPositnNm");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 30);
        
        meta.setColumnName(5, "sMonthxxx");
        meta.setColumnLabel(5, "sMonthxxx");
        meta.setColumnType(5, Types.VARCHAR);
        meta.setColumnDisplaySize(5, 30);
        
        meta.setColumnName(6, "xMCSalesx");
        meta.setColumnLabel(6, "xMCSalesx");
        meta.setColumnType(6, Types.DOUBLE);
        
        meta.setColumnName(7, "xSPSalesx");
        meta.setColumnLabel(7, "xSPSalesx");
        meta.setColumnType(7, Types.DOUBLE);
        
        meta.setColumnName(8, "xServicex");
        meta.setColumnLabel(8, "xServicex");
        meta.setColumnType(8, Types.DOUBLE);
        
        meta.setColumnName(9, "xLTOPoolx");
        meta.setColumnLabel(9, "xLTOPoolx");
        meta.setColumnType(9, Types.DOUBLE);
        
        meta.setColumnName(10, "xDEI2xxxx");
        meta.setColumnLabel(10, "xDEI2xxxx");
        meta.setColumnType(10, Types.DOUBLE);
        
        meta.setColumnName(11, "xCollectn");
        meta.setColumnLabel(11, "xCollectn");
        meta.setColumnType(11, Types.DOUBLE);
     
        meta.setColumnName(12, "xDeductnx");
        meta.setColumnLabel(12, "xDeductnx");
        meta.setColumnType(12, Types.DOUBLE);
        
        p_oIncCateg = new CachedRowSetImpl();
        p_oIncCateg.setMetaData(meta);
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

        meta.setColumnCount(12);
        
        
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
        
        meta.setColumnName(9, "xBranchNm");
        meta.setColumnLabel(9, "xBranchNm");
        meta.setColumnType(9, Types.VARCHAR);
        
        meta.setColumnName(10, "sMonthxxx");
        meta.setColumnLabel(10, "sMonthxxx");
        meta.setColumnType(10, Types.VARCHAR);
        
        meta.setColumnName(11, "sRemarksx");
        meta.setColumnLabel(11, "sRemarksx");
        meta.setColumnType(11, Types.VARCHAR);
        
        meta.setColumnName(12, "cTranStat");
        meta.setColumnLabel(12, "cTranStat");
        meta.setColumnType(12, Types.CHAR);
        meta.setColumnDisplaySize(1, 1);
        
        p_oDetail = new CachedRowSetImpl();
        p_oDetail.setMetaData(meta); 
    }
    private void createDetailEmployee() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();        

        meta.setColumnCount(12);
        
        
        meta.setColumnName(1, "sTransNox");
        meta.setColumnLabel(1, "sTransNox");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);
        
        meta.setColumnName(2, "sEmployID");
        meta.setColumnLabel(2, "sEmployID");
        meta.setColumnType(2, Types.VARCHAR);
        meta.setColumnDisplaySize(2, 12);
        
        meta.setColumnName(3, "xEmployNm");
        meta.setColumnLabel(3, "xEmployNm");
        meta.setColumnType(3, Types.VARCHAR);
        
        meta.setColumnName(4, "xDeductnx");
        meta.setColumnLabel(4, "xDeductnx");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 32);
        
        meta.setColumnName(5, "xIncentve");
        meta.setColumnLabel(5, "xIncentve");
        meta.setColumnType(5, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 32);
        
        meta.setColumnName(6, "nTotalAmt");
        meta.setColumnLabel(6, "nTotalAmt");
        meta.setColumnType(6, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 32);
        
        meta.setColumnName(7, "xPositnNm");
        meta.setColumnLabel(7, "xPositnNm");
        meta.setColumnType(7, Types.VARCHAR);
        
        meta.setColumnName(8, "xBranchNm");
        meta.setColumnLabel(8, "xBranchNm");
        meta.setColumnType(8, Types.VARCHAR);
        
        meta.setColumnName(9, "xBankName");
        meta.setColumnLabel(9, "xBankName");
        meta.setColumnType(9, Types.VARCHAR);
        
        meta.setColumnName(10, "xBankAcct");
        meta.setColumnLabel(10, "xBankAcct");
        meta.setColumnType(10, Types.VARCHAR);
        
        meta.setColumnName(11, "sMonthxxx");
        meta.setColumnLabel(11, "sMonthxxx");
        meta.setColumnType(11, Types.VARCHAR);
        
        p_oDetailEmp = new CachedRowSetImpl();
        p_oDetailEmp.setMetaData(meta); 
    }
    private void createDetailEmployeeNew() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();        

        meta.setColumnCount(12);
        
        
        meta.setColumnName(1, "sTransNox");
        meta.setColumnLabel(1, "sTransNox");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);
        
        meta.setColumnName(2, "sEmployID");
        meta.setColumnLabel(2, "sEmployID");
        meta.setColumnType(2, Types.VARCHAR);
        meta.setColumnDisplaySize(2, 12);
        
        meta.setColumnName(3, "xEmployNm");
        meta.setColumnLabel(3, "xEmployNm");
        meta.setColumnType(3, Types.VARCHAR);
        
        meta.setColumnName(4, "xDeductnx");
        meta.setColumnLabel(4, "xDeductnx");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 32);
        
        meta.setColumnName(5, "xIncentve");
        meta.setColumnLabel(5, "xIncentve");
        meta.setColumnType(5, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 32);
        
        meta.setColumnName(6, "nTotalAmt");
        meta.setColumnLabel(6, "nTotalAmt");
        meta.setColumnType(6, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 32);
        
        meta.setColumnName(7, "xPositnNm");
        meta.setColumnLabel(7, "xPositnNm");
        meta.setColumnType(7, Types.VARCHAR);
        
        meta.setColumnName(8, "xBranchNm");
        meta.setColumnLabel(8, "xBranchNm");
        meta.setColumnType(8, Types.VARCHAR);
        
        meta.setColumnName(9, "xBankName");
        meta.setColumnLabel(9, "xBankName");
        meta.setColumnType(9, Types.VARCHAR);
        
        meta.setColumnName(10, "xBankAcct");
        meta.setColumnLabel(10, "xBankAcct");
        meta.setColumnType(10, Types.VARCHAR);
        
        meta.setColumnName(11, "sMonthxxx");
        meta.setColumnLabel(11, "sMonthxxx");
        meta.setColumnType(11, Types.VARCHAR);
        
        p_oDetailVal = new CachedRowSetImpl();
        p_oDetailVal.setMetaData(meta); 
    }
    private void createDetailMaster() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();        

        meta.setColumnCount(12);
        
        
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
        
        meta.setColumnName(9, "xBranchNm");
        meta.setColumnLabel(9, "xBranchNm");
        meta.setColumnType(9, Types.VARCHAR);
        
        meta.setColumnName(10, "sMonthxxx");
        meta.setColumnLabel(10, "sMonthxxx");
        meta.setColumnType(10, Types.VARCHAR);
        
        meta.setColumnName(11, "sRemarksx");
        meta.setColumnLabel(11, "sRemarksx");
        meta.setColumnType(11, Types.VARCHAR);
        
        meta.setColumnName(12, "cTranStat");
        meta.setColumnLabel(12, "cTranStat");
        meta.setColumnType(12, Types.CHAR);
        meta.setColumnDisplaySize(1, 1);
        
        p_oDetail = new CachedRowSetImpl();
        p_oDetail.setMetaData(meta); 
    }
//  
    private void createBranch() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(3);

        meta.setColumnName(1, "sBranchCd");
        meta.setColumnLabel(1, "sBranchCd");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 4);

        meta.setColumnName(2, "sBranchNm");
        meta.setColumnLabel(2, "sBranchNm");
        meta.setColumnType(2, Types.VARCHAR);
        
        meta.setColumnName(3, "sPeriodxx");
        meta.setColumnLabel(3, "sPeriodxx");
        meta.setColumnType(3, Types.VARCHAR);
        
        p_oBranch = new CachedRowSetImpl();
        p_oBranch.setMetaData(meta);  
    }
    private void createEmployee() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(3);

        meta.setColumnName(1, "sEmployID");
        meta.setColumnLabel(1, "sEmployID");
        meta.setColumnType(1, Types.VARCHAR);

        meta.setColumnName(2, "sEmployID");
        meta.setColumnLabel(2, "sBranchNm");
        meta.setColumnType(2, Types.VARCHAR);
        
        meta.setColumnName(3, "sPeriodxx");
        meta.setColumnLabel(3, "sPeriodxx");
        meta.setColumnType(3, Types.VARCHAR);
        
        p_oBranch = new CachedRowSetImpl();
        p_oBranch.setMetaData(meta);  
    }
    private void createMaster() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(7);

        meta.setColumnName(1, "sTransNox");
        meta.setColumnLabel(1, "sTransNox");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);
        
        meta.setColumnName(2, "sMonthxxx");
        meta.setColumnLabel(2, "sMonthxxx");
        meta.setColumnType(2, Types.VARCHAR);
        meta.setColumnDisplaySize(2, 6);
        
        meta.setColumnName(3, "sRemarksx");
        meta.setColumnLabel(3, "sRemarksx");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 128);
        
        meta.setColumnName(4, "xBranchNm");
        meta.setColumnLabel(4, "xBranchNm");
        meta.setColumnType(4, Types.VARCHAR);
        
        meta.setColumnName(5, "xDeptName");
        meta.setColumnLabel(5, "xDeptName");
        meta.setColumnType(5, Types.VARCHAR);
        
        meta.setColumnName(6, "cTranStat");
        meta.setColumnLabel(6, "cTranStat");
        meta.setColumnType(6, Types.VARCHAR);
        meta.setColumnDisplaySize(6, 1);
        
        meta.setColumnName(7, "xTotalAmt");
        meta.setColumnLabel(7, "xTotalAmt");
        meta.setColumnType(7, Types.DOUBLE);
        
        p_oMaster = new CachedRowSetImpl();
        p_oMaster.setMetaData(meta);
        
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
    
    public String getSQ_Branch(){
        String lsSQL = "";
        String lsStat = String.valueOf(p_nTranStat);
        
           
        lsSQL = "SELECT" + 
                    "  sBranchCd" +
                    ", sBranchNm" +
                    ", '' sPeriodxx" +
                " FROM Branch a" +
                " WHERE cRecdStat = 1";
                    
        
        return lsSQL;
    }
    public String getSQ_Master(){
        String lsSQL = "";
                
        lsSQL = "SELECT" + 
                    "  a.sTransNox" +
                    ", a.sMonthxxx" +
                    ", a.sRemarksx" +
                    ", a.cTranStat" +
                    ", c.sBranchNm xBranchNm" +
                    ", IFNULL(b.sDeptName, '') xDeptName" +
                    ", 0.0 xTotalAmt " +
                    ",  0.0 nMcSalesx " +
                    ",  0.0 nSpareprt " +
                    ",  0.0 nServicex " +
                    ",  0.0 nRegisTri " +
                    ",  0.0 nDei2xxxx " +
                    ",  0.0 xDeductnx " +
                " FROM Incentive_Master a" +
                    " LEFT JOIN Department b " +
                    "   ON a.sDeptIDxx = b.sDeptIDxx" +
                    " LEFT JOIN Branch c " +
                    "   ON LEFT(a.sTransNox,4) = c.sBranchCd" +
                " WHERE " + lsCondition();
        
        return lsSQL;
    }public String getSQ_MasterVal(){
        String lsSQL = "";
                
        lsSQL = "SELECT" + 
                    "  a.sTransNox" +
                    ", a.sMonthxxx" +
                    ", a.sRemarksx" +
                    ", a.cTranStat" +
                    ", c.sBranchNm xBranchNm" +
                    ", IFNULL(b.sDeptName, '') xDeptName" +
                    ", 0.0 xTotalAmt " +
                    ",  0.0 nMcSalesx " +
                    ",  0.0 nSpareprt " +
                    ",  0.0 nServicex " +
                    ",  0.0 nRegisTri " +
                    ",  0.0 nDei2xxxx " +
                    ",  0.0 xDeductnx " +
                " FROM Incentive_Master a" +
                    " LEFT JOIN Department b ON a.sDeptIDxx = b.sDeptIDxx" +
                    ", Branch c " +
                " WHERE " + lsCondition();
        
        return lsSQL;
    }
    
    private String getSQ_Transaction_Total(){
        return "SELECT" +
                    " SUM(nTotalAmt) AS nTotalAmt" +
                " FROM Incentive_Detail";
    }
     private String getSQ_Detail(){
        String lsSQL = "";
        String lsStat = String.valueOf(p_nTranStat);
      
        lsSQL = "SELECT " +
            "  IFNULL(a. sTransNox,'') sTransNox" +
            "  ,IFNULL(b.sEmployID,'') sEmployID" +
            "  ,IFNULL(d.sCompnyNm,'') xEmployNm" +
            "  ,0.0 xIncentve" +
            "  ,0.0 xDeductnx" +
            "  ,IFNULL(b.nTotalAmt,'') nTotalAmt" +
            "  ,IFNULL(e. sPositnNm, '') xPositnNm" +
            "  ,IFNULL(f.sBranchNm,'') xBranchNm" +
            "  ,IFNULL(h.sBankName,'') xBankName" +
            "  ,IFNULL(g.sBankAcct,'') xBankAcct" +
            "  ,IFNULL(a.sMonthxxx,'') sMonthxxx " +
            "FROM Incentive_Master a" +
            "  LEFT JOIN Incentive_Detail b" +
            "    ON a.sTransNox = b.sTransNox" +
            "  LEFT JOIN Employee_Master001 c" +
            "    ON b.sEmployID = c.sEmployID " +
            "  LEFT JOIN Client_Master d" +
            "    ON c.sEmployID   = d.sClientID " +
            "  LEFT JOIN `Position` e" +
            "    ON c.sPositnID   = e.sPositnID " +
            "  LEFT JOIN Branch f" +
            "    ON c.sBranchCd = f.sBranchCd " +
            "  LEFT JOIN Employee_Incentive_Bank_Info g" +
            "    ON b.sEmployID = g.sEmployID   " +
            "  LEFT JOIN Banks h" +
            "    ON g.sBankIDxx = h.sBankIDxx  " +
            "WHERE  " + lsCondition();
        return lsSQL;
    }
     private String getSQ_EmployeeDetail(){
        String lsSQL = "SELECT" +
                            "  a.sTransNox" +
                            ", b.sEmployID" +
                            ", IFNULL(l.sBranchNm, '') xBranchNm" +
                            ", IFNULL(j.sCompnyNm, '') xEmployNm" +
                            ", IFNULL(k.sPositnNm, '') xPositnNm" +
                            ", a.sMonthxxx" +
                            ", IFNULL(m.nInctvAmt, '1B09259D73D06C95C457CB3A89B03299') xMCSalesx" +
                            ", IFNULL(n.nInctvAmt, '1B09259D73D06C95C457CB3A89B03299') xSPSalesx" +
                            ", IFNULL(o.nInctvAmt, '1B09259D73D06C95C457CB3A89B03299') xServicex" +
                            ", IFNULL(p.nInctvAmt, '1B09259D73D06C95C457CB3A89B03299') xLTOPoolx" +
                            ", IFNULL(q.nInctvAmt, '1B09259D73D06C95C457CB3A89B03299') xDEI2xxxx" +
                            ", IFNULL(r.nInctvAmt, '1B09259D73D06C95C457CB3A89B03299') xCollectn" +
                            ", '1B09259D73D06C95C457CB3A89B03299' xDeductnx" +
                            ", IFNULL(c.nAllcPerc, '1B09259D73D06C95C457CB3A89B03299') pMCSalesx" +
                            ", IFNULL(c.nAllcAmtx, '1B09259D73D06C95C457CB3A89B03299') aMCSalesx" +
                            ", IFNULL(d.nAllcPerc, '1B09259D73D06C95C457CB3A89B03299') pSPSalesx" +
                            ", IFNULL(d.nAllcAmtx, '1B09259D73D06C95C457CB3A89B03299') aSPSalesx" + 
                            ", IFNULL(e.nAllcPerc, '1B09259D73D06C95C457CB3A89B03299') pServicex" +
                            ", IFNULL(e.nAllcAmtx, '1B09259D73D06C95C457CB3A89B03299') aServicex" + 
                            ", IFNULL(f.nAllcPerc, '1B09259D73D06C95C457CB3A89B03299') pLTOPoolx" +
                            ", IFNULL(f.nAllcAmtx, '1B09259D73D06C95C457CB3A89B03299') aLTOPoolx" + 
                            ", IFNULL(g.nAllcPerc, '1B09259D73D06C95C457CB3A89B03299') pDEI2xxxx" +
                            ", IFNULL(g.nAllcAmtx, '1B09259D73D06C95C457CB3A89B03299') aDEI2xxxx" + 
                            ", IFNULL(h.nAllcPerc, '1B09259D73D06C95C457CB3A89B03299') pCollectn" +
                            ", IFNULL(h.nAllcAmtx, '1B09259D73D06C95C457CB3A89B03299') aCollectn" +
                        " FROM Incentive_Master a" +
                            ", Incentive_Detail b" +
                                " LEFT JOIN Incentive_Detail_Allocation_Employee c" + 
                                    " LEFT JOIN Incentive_Detail_Allocation m ON c.sTransNox = m.sTransNox AND c.sInctveCD = m.sInctveCD" +
                                " ON b.sTransNox = c.sTransNox AND b.sEmployID = c.sEmployID AND c.sInctveCD = '001'" +
                                " LEFT JOIN Incentive_Detail_Allocation_Employee d" + 
                                    " LEFT JOIN Incentive_Detail_Allocation n ON d.sTransNox = n.sTransNox AND d.sInctveCD = n.sInctveCD" +
                                " ON b.sTransNox = d.sTransNox AND b.sEmployID = d.sEmployID AND d.sInctveCD = '002'" +
                                " LEFT JOIN Incentive_Detail_Allocation_Employee e" +
                                    " LEFT JOIN Incentive_Detail_Allocation o ON e.sTransNox = o.sTransNox AND e.sInctveCD = o.sInctveCD" +
                                " ON b.sTransNox = e.sTransNox AND b.sEmployID = e.sEmployID AND e.sInctveCD = '003'" +
                                " LEFT JOIN Incentive_Detail_Allocation_Employee f" + 
                                    " LEFT JOIN Incentive_Detail_Allocation p ON f.sTransNox = p.sTransNox AND f.sInctveCD = p.sInctveCD" +
                                " ON b.sTransNox = f.sTransNox AND b.sEmployID = f.sEmployID AND f.sInctveCD = '004'" +
                                " LEFT JOIN Incentive_Detail_Allocation_Employee g" + 
                                    " LEFT JOIN Incentive_Detail_Allocation q ON g.sTransNox = q.sTransNox AND g.sInctveCD = q.sInctveCD" +
                                " ON b.sTransNox = g.sTransNox AND b.sEmployID = g.sEmployID AND g.sInctveCD = '005'" +
                                " LEFT JOIN Incentive_Detail_Allocation_Employee h" +
                                    " LEFT JOIN Incentive_Detail_Allocation r ON h.sTransNox = r.sTransNox AND h.sInctveCD = r.sInctveCD" +
                                " ON b.sTransNox = h.sTransNox AND b.sEmployID = h.sEmployID AND h.sInctveCD = '006'" +
                            ", Employee_Master001 i" +  
                                " LEFT JOIN Client_Master j" + 
                                    " ON i.sEmployID = j.sClientID" + 
                                " LEFT JOIN `Position` k" + 
                                    " ON i.sPositnID = k.sPositnID" + 
                                " LEFT JOIN Branch l" +
                                    " ON i.sBranchCd = l.sBranchCd" +
                        " WHERE a.sTransNox = b.sTransNox" + 
                            " AND b.sEmployID = i.sEmployID";
        
        return MiscUtil.addCondition(lsSQL, lsCondition());
    }
    private String getSQ_MasterDetail(){
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
                    ",  0.0 nMcSalesx " +
                    ",  0.0 nSpareprt " +
                    ",  0.0 nServicex " +
                    ",  0.0 nRegisTri " +
                    ",  0.0 nDei2xxxx " +
                " FROM Incentive_Detail a" +
                    ", Employee_Master001 b" +
                        " LEFT JOIN Client_Master c ON b.sEmployID = c.sClientID" +
                        " LEFT JOIN Employee_Level d ON b.sEmpLevID = d.sEmpLevID" +
                        " LEFT JOIN `Position` e ON b.sPositnID = e.sPositnID" +
                " WHERE a.sEmployID = b.sEmployID" +
                " ORDER BY nEntryNox";
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
                " FROM Incentive_Detail_Ded_Allocation";
    }
    
    private String getSQ_Detail_Deduction_Emp(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", a.nEntryNox" +
                    ", a.sEmployID" +
                    ", a.nAllcPerc" +
                    ", a.nAllcAmtx" +
                    ", IFNULL(b.sCompnyNm, '') xEmployNm" +
                    ", 0.00 nTotalAmt" +
                " FROM Incentive_Detail_Ded_Allocation_Employee a" +
                    " LEFT JOIN Client_Master b ON a.sEmployID = b.sClientID";
    }
    private String getSQ_Record(){
        String lsSQL = "";
        lsSQL =  "SELECT" +
                    "  a.sEmployID" +
                    ", b.sCompnyNm" +
                    ", c.sBranchNm" +
                    ", a.sBranchCd" +
                " FROM Employee_Master001 a" +
                    ", Client_Master b" +
                    ", Branch c" +
                " WHERE a.sEmployID = b.sClientID" +
                    " AND a.sBranchCd = c.sBranchCd" +
                    " AND a.cRecdStat = '1'" +
                    " AND ISNULL(a.dFiredxxx)";
        
        return lsSQL;
    }
    public boolean searchEmployee(String fsValue, boolean fbByCode) throws SQLException{
       
        String lsSQL = "SELECT" +
                            "  a.sEmployID" +
                            ", b.sCompnyNm" +
                            ", c.sBranchNm" +
                            ", c.sBranchCD" +
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
                
                return OpenRecord((String) loJSON.get("sEmployID")) &&
                        OpenBranch((String) loJSON.get("sBranchCD"));
            }else{     
                return false;
            }
            
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
                return OpenRecord((String) loJSON.get("sEmployID"));
        }else{
            return false;
        }
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
        p_oEmployee = factory.createCachedRowSet();
        p_oEmployee.populate(loRS);
        MiscUtil.close(loRS);
        
        if (p_oEmployee.size() == 0) return false;
        p_nEditMode = EditMode.READY;
        return true;
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
    private String lsCondition(){
        String lsStat = String.valueOf(p_nTranStat);
        String lsCondition = "";
        if (lsStat.length() > 1){
            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
                lsCondition += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            
            lsCondition = " a.cTranStat IN (" + lsCondition.substring(2) + ")";
        } else{  
            if(lsStat.equals("3")){
                lsCondition = " a.cTranStat = '3'";
            }else{
                lsCondition = " a.cTranStat = '1'";
            }
            
        }
        System.out.println("department  = " + p_oApp.getDepartment());
        if (MAIN_OFFICE.contains(p_oApp.getBranchCode())){            
                if ((AUDITOR + "»" + COLLECTION + "»" + FINANCE).contains(p_oApp.getDepartment())){
                    if (p_oApp.getDepartment().equals(AUDITOR)){
                        if(lsStat.equals("1")){
                            lsCondition = lsCondition + " AND a.cApprovd2 = '0'";
                        }else if(lsStat.equals("2")){
                            lsCondition = lsCondition + " AND a.cApprovd2 = '1'";
                        }
                    }
                }else{
                    lsCondition = lsCondition + " AND LEFT(a.sTransNox,4) = " + SQLUtil.toSQL(p_oApp.getBranchCode());
                }
            }else{
               lsCondition = lsCondition + " AND LEFT(a.sTransNox,4)  = " + SQLUtil.toSQL(p_oApp.getBranchCode());
                
            }
        return lsCondition;
    }
    private void loadConfig(){
        //update the value on configuration before deployment
        System.setProperty(DEBUG_MODE, "0"); 
        System.setProperty(REQUIRE_CSS, "0");
        System.setProperty(REQUIRE_CM, "1");
        System.setProperty(REQUIRE_BANK_ON_APPROVAL, "0");
    }
    
    private void computeEmpTotalIncentivesAmount() throws SQLException{
        
        double lnPercentx,  lnIncentve, lnDeductnx;   
        double lnInctvAmt;
        int lnCtr;
         for (lnCtr = 1; lnCtr <= getCategoryCount(); lnCtr++){
            p_oDetailCateg.absolute(lnCtr);
            lnIncentve = 0.0;
            lnPercentx = 0.0;
            lnInctvAmt = 0.00;
            if (p_oDetailCateg.getDouble("xInctvPrc") > 0.00){
                lnInctvAmt = DecryptAmount(p_oDetailCateg.getString("xInctvAmt"));
                lnInctvAmt = lnInctvAmt * p_oDetailCateg.getDouble("xInctvPrc") / 100;
            
            }else{
                lnInctvAmt = DecryptAmount(p_oDetailCateg.getString("xIncentve"));
            }
            p_oDetailCateg.updateString("xIncentve", EncryptAmount(lnInctvAmt));
            
//            p_oDetailCateg.updateString("xDeductnx", EncryptAmount(lnDeductnx));
//            p_oDetailCateg.updateString("nTotalAmt", EncryptAmount(lnInctvAmt));
            p_oDetailCateg.updateRow();
           
        
         }
    }
    
    
    
}
