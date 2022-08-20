/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.fund.manager.base;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.StringUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.constants.UserRight;
import org.rmj.fund.manager.parameters.IncentiveBankInfo;

/**
 *
 * @author User
 */
public class DeptIncentiveRelease {
    private final String FINANCE = "028";
    private final String DEBUG_MODE = "app.debug.mode";
    private final String REQUIRE_CSS = "app.require.css.approval";
    private final String REQUIRE_CM = "app.require.cm.approval";
    private final String MASTER_TABLE = "Incentive_Releasing_Master";
    private final String DEPT_MASTER_TABLE = "Incentive_Releasing_Master";
    private final String DEPT_DETAIL_TABLE = "Incentive_Releasing_Master";
    
    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    private final DeptIncentive p_oIncentive;
    private ArrayList<CachedRowSet> p_oDetail;
    private ArrayList<String> p_oTag;
    
    
    private String p_sBranchCd;
    private int p_nEditMode;
    private int p_nTranStat;

    private String p_sMessage;
    private boolean p_bWithUI = true;
    
    private CachedRowSet p_oMaster;
    private CachedRowSet p_oDeptMaster;
    private CachedRowSet p_oDeptDetail;
    private LMasDetTrans p_oListener;
    
    public DeptIncentiveRelease(GRider foApp, String fsBranchCd, boolean fbWithParent){
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;        
                
        if (p_sBranchCd.isEmpty()) p_sBranchCd = p_oApp.getBranchCode();
        p_oIncentive = new DeptIncentive(p_oApp, p_sBranchCd, true);
        p_oIncentive.setTranStat(1);
        
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
    
    public String getMessage(){
        return p_sMessage;
    }
    
    private void loadConfig(){
        //update the value on configuration before deployment
        System.setProperty(DEBUG_MODE, "1"); 
        System.setProperty(REQUIRE_CSS, "1");
        System.setProperty(REQUIRE_CM, "1");
    }
    private void createDetail() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();        

        meta.setColumnCount(11);
        
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
        meta.setColumnDisplaySize(9, 128);
        
        meta.setColumnName(10, "xBankAcct");
        meta.setColumnLabel(10, "xBankAcct");
        meta.setColumnType(10, Types.VARCHAR);
        meta.setColumnDisplaySize(10, 128);
        
        meta.setColumnName(11, "xBankName");
        meta.setColumnLabel(11, "xBankName");
        meta.setColumnType(11, Types.VARCHAR);
        meta.setColumnDisplaySize(11, 128);
        
        p_oDeptDetail = new CachedRowSetImpl();
        p_oDeptDetail.setMetaData(meta);        
        
//     
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
        
        p_oDeptMaster = new CachedRowSetImpl();
        p_oDeptMaster.setMetaData(meta);
        
        p_oDeptMaster.last();
        p_oDeptMaster.moveToInsertRow();
        
        MiscUtil.initRowSet(p_oDeptMaster);       
        p_oTag = new ArrayList<>();
       ResultSet loRS = p_oApp.executeQuery(getSQ_DeptMaster());
       int lnRow = 1;
       while (loRS.next()){
            p_oDeptMaster.last();
            p_oDeptMaster.moveToInsertRow();

            MiscUtil.initRowSet(p_oDeptMaster);        
            p_oDeptMaster.updateString("sTransNox", loRS.getString("sTransNox"));
            p_oDeptMaster.updateString("dTransact", loRS.getString("dTransact"));
            p_oDeptMaster.updateString("sDeptIDxx", loRS.getString("sDeptIDxx"));
            p_oDeptMaster.updateString("sInctveCD", loRS.getString("sInctveCD"));
            p_oDeptMaster.updateString("sRemarksx", loRS.getString("sRemarksx"));
            p_oDeptMaster.updateString("dEffctive", loRS.getString("dEffctive"));
            p_oDeptMaster.updateString("cTranStat", loRS.getString("cTranStat"));
            p_oDeptMaster.updateString("sApproved", loRS.getString("sApproved"));
            p_oDeptMaster.updateString("dApproved", loRS.getString("dApproved"));
            p_oDeptMaster.updateString("sModified", loRS.getString("sModified"));
            p_oDeptMaster.updateString("dModified", loRS.getString("dModified"));
            p_oDeptMaster.updateString("xBranchNm", loRS.getString("xBranchNm"));
            p_oDeptMaster.updateString("xDeptName", loRS.getString("xDeptName"));
            p_oDeptMaster.updateString("xInctvNme", loRS.getString("xInctvNme"));
            
            p_oTag.add("0");
            p_oDeptMaster.insertRow();
            p_oDeptMaster.moveToCurrentRow();
            lnRow++;
        }
    }
    private void initMaster() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(10);

        meta.setColumnName(1, "sTransNox");
        meta.setColumnLabel(1, "sTransNox");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);

        meta.setColumnName(2, "dTransact");
        meta.setColumnLabel(2, "dTransact");
        meta.setColumnType(2, Types.DATE);

        meta.setColumnName(3, "sApproved");
        meta.setColumnLabel(3, "sApproved");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 12);

        meta.setColumnName(4, "sPostedxx");
        meta.setColumnLabel(4, "sPostedxx");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 12);
        
        meta.setColumnName(5, "dPostedxx");
        meta.setColumnLabel(5, "dPostedxx");
        meta.setColumnType(5, Types.DATE);
        
        meta.setColumnName(6, "nTranTotl");
        meta.setColumnLabel(6, "nTranTotl");
        meta.setColumnType(6, Types.DECIMAL);
        
        meta.setColumnName(7, "nEntryNox");
        meta.setColumnLabel(7, "nEntryNox");
        meta.setColumnType(7, Types.INTEGER);
        
        meta.setColumnName(8, "cTranStat");
        meta.setColumnLabel(8, "cTranStat");
        meta.setColumnType(8, Types.VARCHAR);
        meta.setColumnDisplaySize(8, 1);
        
        meta.setColumnName(9, "sModified");
        meta.setColumnLabel(9, "sModified");
        meta.setColumnType(9, Types.VARCHAR);
        meta.setColumnDisplaySize(9, 12);
        
        meta.setColumnName(10, "dModified");
        meta.setColumnLabel(10, "dModified");
        meta.setColumnType(10, Types.DATE);
        
        p_oMaster = new CachedRowSetImpl();
        p_oMaster.setMetaData(meta);
        
        p_oMaster.last();
        p_oMaster.moveToInsertRow();
        
        MiscUtil.initRowSet(p_oMaster);       
        p_oMaster.updateObject("sTransNox", MiscUtil.getNextCode(MASTER_TABLE, "sTransNox", true, p_oApp.getConnection(), p_sBranchCd));
        p_oMaster.updateObject("dTransact", p_oApp.getServerDate());
        p_oMaster.updateString("cTranStat", TransactionStatus.STATE_OPEN);
        
        p_oMaster.insertRow();
        p_oMaster.moveToCurrentRow();
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
        lsSQL = MiscUtil.addCondition(getSQ_DeptMaster(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDeptMaster = factory.createCachedRowSet();
        p_oDeptMaster.populate(loRS);
        MiscUtil.close(loRS);
        
        //open detail
        lsSQL = MiscUtil.addCondition(getSQ_DeptDetail(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDeptDetail = factory.createCachedRowSet();
        p_oDeptDetail.populate(loRS);
        MiscUtil.close(loRS);
        
       
        
//        computeEmpTotalIncentiveAmount();
        p_nEditMode = EditMode.READY;
        
        return true;
    }
    
    
    private String getSQ_Master(){
        return "SELECT" +
                    "  sTransNox" +
                    ", dTransact" +
                    ", sApproved" +
                    ", sPostedxx" +
                    ", dPostedxx" +
                    ", nTranTotl" +
                    ", nEntryNox" +
                    ", cTranStat" +
                    ", sModified" +
                    ", dModified" +
                " FROM Incentive_Releasing_Master";
    }
    public String getSQ_DeptMaster(){
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
    
    private String getSQ_DeptDetail(){
        return " SELECT  " +
            "  IFNULL(a.sTransNox,'') sTransNox  " +
            ", IFNULL(a.nEntryNox,'') nEntryNox  " +
            ", IFNULL(a.sEmployID,'') sEmployID  " +
            ", IFNULL(a.dLastUpdt,'') dLastUpdt  " +
            ", IFNULL(a.sOldAmtxx,'0') sOldAmtxx  " +
            ", IFNULL(a.sNewAmtxx,'0') sNewAmtxx  " +
            ", IFNULL(a.sRemarksx,'') sRemarksx  " +
            ", IFNULL(c.sCompnyNm,'') xEmployNm   " +
            ", IFNULL(e.sPositnNm, '') xPositnNm   " +
            ", IFNULL(d.sBankAcct, '') xBankAcct  " +
            ", IFNULL(f.sBankName, '') xBankName   " +
            " FROM Department_Incentive_Detail a  " +
            ", Employee_Master001 b   " +
            "     LEFT JOIN Client_Master c ON b.sEmployID = c.sClientID  " +
            "     LEFT JOIN `Position` e ON b.sPositnID = e.sPositnID  " +
            "     LEFT JOIN Employee_Incentive_Bank_Info d ON b.sEmployID = d.sEmployID  " +
            "     LEFT JOIN `Banks` f ON d.sBankIDxx = f.sBankIDxx  " +
            " WHERE a.sEmployID = b.sEmployID   " +
            " GROUP BY a.sEmployID   " +
            " ORDER BY sTransNox,xEmployNm";
    }
    
    public int getDetailItemCount() throws SQLException{
//        p_oDeptDetail.last();
//        return p_oDeptDetail.getRow();
        return p_oDetail.size();
    }
    
    public Object getDeptDetail(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getItemCount() == 0 || fnRow > getItemCount()) return null;
        
        p_oDeptDetail.absolute(fnRow);
       
        switch (fnIndex){
            case 5://sOldAmtxx
            case 6://sNewAmtxx
                return p_oDeptDetail.getObject(fnIndex);
            default:
                return p_oDeptDetail.getObject(fnIndex);
        }

//        return p_oDetail.get(fnRow);
    }
    
    public Object getDeptDetail(int fnRow, String fsIndex) throws SQLException{
        return getDeptDetail(fnRow, getColumnIndex(p_oDeptDetail, fsIndex));
    }
//     public void setDeptDetail(int fnRow, int fnIndex, Object foValue) throws SQLException{
//        if (getDetailItemCount()== 0 || fnRow > getDetailItemCount()) return;
//        
//        p_oDeptDetail.absolute(fnRow);
//        switch (fnIndex){
//            case 6://sNewAmtxx
//                if (StringUtil.isNumeric(String.valueOf(foValue))) 
//                    p_oDeptDetail.updateObject(fnIndex, (double) foValue);
//                else
//                    p_oDeptDetail.updateObject(fnIndex, (0.00));
//
////                
////                if (p_oListener != null) p_oListener.DetailRetreive(fnRow,fnIndex,p_oDetail.getString(fnIndex));
//                break;
//            case 7://sRemarksx
//            
//                p_oDeptDetail.updateString(fnIndex, (String) foValue);
//
////                if (p_oListener != null) p_oListener.DetailRetreive(fnRow,fnIndex, p_oDetail.getString(fnIndex));
//                break;
//        }
//        
//        p_oDeptDetail.updateRow();
//        if (p_oListener != null) p_oListener.DetailRetreive(0,0,"");
//    }
    public int getItemCount() throws SQLException{
        p_oDeptMaster.last();
        return p_oDeptMaster.getRow();
    }
    
    
    public Object getDeptMaster(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getItemCount() == 0 || fnRow > getItemCount()) return null;
        
        p_oDeptMaster.absolute(fnRow);
        return p_oDeptMaster.getObject(fnIndex);
    }
    
    public Object getDeptMaster(int fnRow, String fsIndex) throws SQLException{
        return getDeptMaster(fnRow, getColumnIndex(p_oDeptMaster, fsIndex));
    }
    
    public void setTag(int fnRow, boolean fbValue) throws SQLException{
        if (getItemCount() == 0 || getItemCount() < fnRow) return;
        
        p_oTag.set(fnRow, fbValue ? "1" : "0");
    }
    
    public boolean getTag(int fnRow) throws SQLException{
        if (getItemCount() == 0 || getItemCount() < fnRow) return false;
        
        return p_oTag.get(fnRow).equals("1");
    }
    
    public Object getMaster(int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oMaster.first();
        return p_oMaster.getObject(fnIndex);
    }
    
    public Object getMaster(String fsIndex) throws SQLException{
        return getMaster(getColumnIndex(p_oMaster, fsIndex));
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
    public boolean NewTransaction() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        if (!System.getProperty(DEBUG_MODE).equals("1")){
            if (p_oApp.getDepartment().equals(FINANCE)){
                p_sMessage = "User is not allowed to use this application.";
                return false;
            }
        }
        
        p_sMessage = "";
        
        initMaster();
        createMaster();
        
        
        p_nEditMode = EditMode.ADDNEW;
        return true;
    }
    public boolean SearchDeptTransaction(String fsValue, boolean fbByCode) throws SQLException{
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
        
        String lsSQL = getSQ_DeptMaster();
        String lsCondition = "";
        
//        if (MAIN_OFFICE.contains(p_oApp.getBranchCode())){            
//            if (!(AUDITOR + "»" + COLLECTION + "»" + FINANCE).contains(p_oApp.getDepartment()))
//                lsCondition = "a.sDeptIDxx = " + SQLUtil.toSQL(p_oApp.getDepartment());
//        } else
//            lsCondition = "a.sDeptIDxx LIKE " + SQLUtil.toSQL(p_oApp.getDepartment() + "%");
//        
//        if (!lsCondition.isEmpty()) lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
//        
        if (p_bWithUI){
            JSONObject loJSON = showFXDialog.jsonSearch(
                                    p_oApp, 
                                    lsSQL, 
                                    fsValue, 
                                    "Trans. No.»Department»Date Effective»Remarks", 
                                    "sTransNox»xDeptName»dEffctive»sRemarksx", 
                                    "a.sTransNox»b.sDeptName»a.dEffctive»a.sRemarksx", 
                                    fbByCode ? 0 : 1);
            System.out.println(fsValue);
                if (loJSON != null) 
                    return OpenDeptTransaction((String) loJSON.get("sTransNox"));
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
        
        return OpenDeptTransaction(lsSQL);
    }
    
    public boolean OpenDeptTransaction(String fsTransNox) throws SQLException{
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
//        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        createDetail();
      
//        
//        //open detail
//        lsSQL = MiscUtil.addCondition(getSQ_DeptDetail(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
//        loRS = p_oApp.executeQuery(lsSQL);
//        p_oDeptDetail = factory.createCachedRowSet();
//        p_oDeptDetail.populate(loRS);
//        MiscUtil.close(loRS);
        lsSQL = MiscUtil.addCondition(getSQ_DeptDetail(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        p_oDetail = new ArrayList();
        int lnRow = 1;
        while (loRS.next()){
            p_oDeptDetail.last();
            p_oDeptDetail.moveToInsertRow();

            MiscUtil.initRowSet(p_oDeptDetail);        
            p_oDeptDetail.updateString("nEntryNox", loRS.getString("nEntryNox"));
            p_oDeptDetail.updateString("sEmployID", loRS.getString("sEmployID"));
            p_oDeptDetail.updateString("xEmployNm", loRS.getString("xEmployNm"));
//            p_oDetail.updateString("xEmpLevNm", loRS.getString("xEmpLevNm"));
            p_oDeptDetail.updateString("xPositnNm", loRS.getString("xPositnNm"));
            p_oDeptDetail.updateString("xBankAcct", loRS.getString("xBankAcct"));
            p_oDeptDetail.updateString("xBankName", loRS.getString("xBankName"));
            p_oDeptDetail.updateString("dLastUpdt", loRS.getString("xBankName"));
            p_oDeptDetail.updateString("sOldAmtxx", loRS.getString("sOldAmtxx"));
            p_oDeptDetail.updateString("sNewAmtxx", loRS.getString("sNewAmtxx"));
            
            
            p_oDeptDetail.insertRow();
            p_oDeptDetail.moveToCurrentRow();
            
            p_oDetail.add(p_oDeptDetail);
            System.out.println(lnRow + " " + loRS.getString("xEmployNm") + " "+ loRS.getString("sOldAmtxx")+ " "+ loRS.getString("sNewAmtxx"));
            lnRow++;
        }
         MiscUtil.close(loRS);
//        computeEmpTotalIncentiveAmount();
        p_nEditMode = EditMode.READY;
        
        return true;
    }
    
}
