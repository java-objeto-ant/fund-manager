/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.fund.manager.base;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
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
import org.rmj.appdriver.GSec;
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
 *
 * @author User
 */
public class BankInfoReport {
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
    
    
    private String p_sBranchCd;
    private int p_nEditMode;
    private int p_nTranStat;

    private String p_sMessage;
    private boolean p_bWithUI = true;

    private CachedRowSet p_oBranch;
    private CachedRowSet p_oDeparment;
    private CachedRowSet p_oMaster;
    private CachedRowSet p_oDetail;
    private CachedRowSet p_oRecord;
    private LMasDetTrans p_oListener;
   
    public BankInfoReport(GRider foApp, String fsBranchCd, boolean fbWithParent){        
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;        
                
        if (p_sBranchCd.isEmpty()) p_sBranchCd = p_oApp.getBranchCode();
        
        loadConfig();
        
        p_nTranStat = 0;
        p_nEditMode = EditMode.UNKNOWN;
//        try {
//            createDepartment();
//            createBranch();
//        } catch (SQLException ex) {
//            Logger.getLogger(BankInfoReport.class.getName()).log(Level.SEVERE, null, ex);
//        }
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
   
    public Object getDepartment(int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oDeparment.first();
        return p_oDeparment.getObject(fnIndex);
    }
    
    public Object getDepartment(String fsIndex) throws SQLException{
        return getDepartment(getColumnIndex(p_oDeparment, fsIndex));
    }
    public void setDepartment(){
        p_oDeparment = null;
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
   
    public Object getDetail(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oDetail.absolute(fnRow);
        return p_oDetail.getObject(fnIndex);
    }
    
    public Object getDetail(int fnRow, String fsIndex) throws SQLException{
        return getDetail(fnRow, getColumnIndex(p_oDetail, fsIndex));
    }
    
    public int getItemCount() throws SQLException{
        p_oDetail.last();
        return p_oDetail.getRow();
    }
    
    public String getMessage(){
        return p_sMessage;
    }
    private void createDetail() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(5);
         
        meta.setColumnName(1, "sBranchNm");
        meta.setColumnLabel(1, "sBranchNm");
        meta.setColumnType(1, Types.VARCHAR);

        meta.setColumnName(2, "sCompnyNm");
        meta.setColumnLabel(2, "sCompnyNm");
        meta.setColumnType(2, Types.VARCHAR);


        meta.setColumnName(3, "sBankName");
        meta.setColumnLabel(3, "sBankName");
        meta.setColumnType(3, Types.VARCHAR);

        meta.setColumnName(4, "sBankAcct");
        meta.setColumnLabel(4, "sBankAcct");
        meta.setColumnType(4, Types.VARCHAR);
        
        meta.setColumnName(4, "sDeptName");
        meta.setColumnLabel(4, "sDeptName");
        meta.setColumnType(4, Types.VARCHAR);

        
        p_oDetail = new CachedRowSetImpl();
        p_oDetail.setMetaData(meta);
    }
    
    
    private void createDepartment() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(3);

        meta.setColumnName(1, "sDeptIDxx");
        meta.setColumnLabel(1, "sDeptIDxx");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 3);

        meta.setColumnName(2, "sDeptName");
        meta.setColumnLabel(2, "sDeptName");
        meta.setColumnType(2, Types.VARCHAR);
        
        meta.setColumnName(3, "sRecdStat");
        meta.setColumnLabel(3, "sRecdStat");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 1);
        
        p_oDeparment = new CachedRowSetImpl();
        p_oDeparment.setMetaData(meta);  
    }
    private void createBranch() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(3);

        meta.setColumnName(1, "sBranchCd");
        meta.setColumnLabel(1, "sBranchCd");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 3);

        meta.setColumnName(2, "sBranchNm");
        meta.setColumnLabel(2, "sBranchNm");
        meta.setColumnType(2, Types.VARCHAR);
        
        meta.setColumnName(3, "sRecdStat");
        meta.setColumnLabel(3, "sRecdStat");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 1);
        
        p_oBranch = new CachedRowSetImpl();
        p_oBranch.setMetaData(meta);  
    }
    public boolean searchBranch(String fsValue, boolean fbByCode) throws SQLException{
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
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchCd= " + SQLUtil.toSQL(fsValue));   
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
    public boolean searchDepartment(String fsValue, boolean fbByCode) throws SQLException{
        createDepartment();
        String lsSQL = getSQ_Department();
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sDeptIDxx = " + SQLUtil.toSQL(fsValue));
        else
            lsSQL = MiscUtil.addCondition(lsSQL, "sDeptName LIKE " + SQLUtil.toSQL(fsValue + "%"));
        
      
        JSONObject loJSON;
        
        if (p_bWithUI){
            loJSON = showFXDialog.jsonSearch(
                        p_oApp, 
                        lsSQL, 
                        fsValue, 
                        "Code»Department Name", 
                        "sDeptIDxx»sDeptName", 
                        "sDeptIDxx»sDeptName", 
                        fbByCode ? 0 : 1);
            
            if (loJSON != null) 
                return OpenDepartment((String) loJSON.get("sDeptIDxx"));
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sDeptIDxx = " + SQLUtil.toSQL(fsValue));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "sDeptName LIKE " + SQLUtil.toSQL(fsValue + "%")); 
            lsSQL += " LIMIT 1";
        }
        
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        if (!loRS.next()){
            MiscUtil.close(loRS);
            p_sMessage = "No bracnh found for the givern criteria.";
            return false;
        }
        
        lsSQL = loRS.getString("sDeptIDxx");
        MiscUtil.close(loRS);
        
        return OpenDepartment(lsSQL);
    }
    public boolean OpenRecord() throws SQLException{
        p_nEditMode = EditMode.UNKNOWN;
        
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        createDetail();
        p_sMessage = "";
        
//        if (!isEntryOK()) return false;
        String lsCondition = "";
        if(p_oBranch != null){
            lsCondition = lsCondition + " AND e.sBranchCd = " + SQLUtil.toSQL((String)getBranch("sBranchCd"));
        }
        if(p_oDeparment != null){
            lsCondition = lsCondition + " AND f.sDeptIDxx = " + SQLUtil.toSQL((String)getDepartment("sDeptIDxx"));
        }
        String lsSQL ="";
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        //open master
        lsSQL =  getSQ_Record() + lsCondition +" ORDER BY e.sBranchNm";
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDetail = factory.createCachedRowSet();
        p_oDetail.populate(loRS);
        MiscUtil.close(loRS);
        
        if (p_oDetail.size() == 0) return false;
        p_nEditMode = EditMode.READY;
        return true;
    }
     public boolean OpenDepartment(String fsBranch) throws SQLException{
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
        lsSQL = MiscUtil.addCondition(getSQ_Department(), "sDeptIDxx = " + SQLUtil.toSQL(fsBranch));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDeparment = factory.createCachedRowSet();
        p_oDeparment.populate(loRS);
        MiscUtil.close(loRS);
        
        p_nEditMode = EditMode.READY;
        
        return true;
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
    
    private String getSQ_Record(){
        String lsSQL = "";
        String lsStat = String.valueOf(p_nTranStat);
        
        if (lsStat.length() > 1){
            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
                lsSQL += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            
            lsSQL = " AND a.cRecdStat IN (" + lsSQL.substring(2) + ")";
        } else 
            lsSQL = " AND a.cRecdStat = " + SQLUtil.toSQL(lsStat);
        
        lsSQL =  "SELECT "
                + "IFNULL(e.sBranchNm,'') sBranchNm, "
                + "IFNULL(c.sCompnyNm,'') sCompnyNm, "
                + "IFNULL(d.sBankName,'') sBankName, "
                + "IFNULL(a.sBankAcct,'') sBankAcct, "
                + "IFNULL(f.sDeptName,'') sDeptName " 
                + "FROM Employee_Incentive_Bank_Info a "
                + "   LEFT JOIN Client_Master c "
                + "     ON a.sEmployID = c.sClientID "
                + "   LEFT JOIN Banks d ON a.sBankIDxx = d.sBankIDxx " 
                + ", Employee_Master001 b "
                + "   LEFT JOIN Branch e "
                + "         ON b.sBranchCd = e.sBranchCd "
                + "   LEFT JOIN Department f "
                + "         ON b.sDeptIDxx = f.sDeptIDxx "
                + "WHERE a.sEmployID = b.sEmployID " +
                    lsSQL;
        
        return lsSQL;
    }
    public String getSQ_Department(){
        String lsSQL = "";
        String lsStat = String.valueOf(p_nTranStat);
           
        lsSQL = "SELECT" + 
                    "  sDeptIDxx" +
                    ", sDeptName" +
                    ", cRecdStat" +
                " FROM Department a" +
                " WHERE cRecdStat = 1";
                    
        
        return lsSQL;
    }
    public String getSQ_Branch(){
        String lsSQL = "";
        String lsStat = String.valueOf(p_nTranStat);
           
        lsSQL = "SELECT" + 
                    "  sBranchCd" +
                    ", sBranchNm" +
                    ", cRecdStat" +
                " FROM Branch a" +
                " WHERE cRecdStat = 1";
                    
        
        return lsSQL;
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
        
        
        //validate branch code
        if (p_oMaster.getString("sBranchCd") == null){
            p_sMessage = "No employee detected.";
            return false;
        }
        //validate department
        
        if (p_oMaster.getString("sDeptIDxx") == null){
            p_sMessage = "No employee detected.";
            return false;
        }
        
        
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
    
    
    private void loadConfig(){
        //update the value on configuration before deployment
        System.setProperty(DEBUG_MODE, "0"); 
        System.setProperty(REQUIRE_CSS, "0");
        System.setProperty(REQUIRE_CM, "1");
        System.setProperty(REQUIRE_BANK_ON_APPROVAL, "0");
    }
}
