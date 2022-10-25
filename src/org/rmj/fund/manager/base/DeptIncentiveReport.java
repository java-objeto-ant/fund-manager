/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.fund.manager.base;

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
import org.rmj.appdriver.GSec;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.MySQLAESCrypt;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.UserRight;
import org.rmj.fund.manager.parameters.IncentiveBankInfo;

/**
 *
 * @author User
 */
public class DeptIncentiveReport {
    private final String DEBUG_MODE = "app.debug.mode";

    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    private final IncentiveBankInfo p_oBankInfo;
    
    private String p_sBranchCd;
    private int p_nEditMode;
    private int p_nTranStat;

    private String p_sMessage;
    private boolean p_bWithUI = true;

    private CachedRowSet p_oDeparment;
    private CachedRowSet p_oMaster;
    private CachedRowSet p_oDetail;
    
    private LMasDetTrans p_oListener;
   
    public DeptIncentiveReport(GRider foApp, String fsBranchCd, boolean fbWithParent){        
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
   
    public boolean OpenTransaction(String fsValue) throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        createDetail();
        p_sMessage = "";
        
        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        String lsCondition = "";
        String lsCondition1 = "";
        String lsCondition2 = "";
        
        if(p_oDeparment != null){
            lsCondition = " AND g.sDeptIDxx = " + SQLUtil.toSQL(getDepartment("sDeptIDxx"));
        }
        
        if(!fsValue.isEmpty()){
           lsCondition =  lsCondition + " AND g.dEffctive LIKE " +SQLUtil.toSQL(fsValue + "%");
        }
        lsSQL = getSQ_Detail() + lsCondition +
            " GROUP BY a.sEmployID   " +
            " ORDER BY sTransNox,xEmployNm,xDeptName";
        
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDetail = factory.createCachedRowSet();
        p_oDetail.populate(loRS);
        MiscUtil.close(loRS);
        p_nEditMode = EditMode.READY;
        return true;
    }
    public boolean OpenTransactionMaster(String fsValue) throws SQLException{
         
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        createDetail();
        p_sMessage = "";

        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        String lsCondition = "";
        String lsCondition1 = "";
        String lsCondition2 = "";
        if(p_oDeparment != null){
            lsCondition = " AND a.sDeptIDxx LIKE " + SQLUtil.toSQL(getDepartment("sDeptIDxx"));
        }
        
        if(!fsValue.isEmpty()){
           lsCondition =  lsCondition + " AND a.dEffctive LIKE " +SQLUtil.toSQL(fsValue + "%");
        }
        lsSQL = getSQ_Master() + lsCondition +  " ORDER BY sDeptIDxx, sTransNox";
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oMaster = factory.createCachedRowSet();
        p_oMaster.populate(loRS);
        MiscUtil.close(loRS);
        p_nEditMode = EditMode.READY;
        return true;
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
        
        double transTotal = 0;
        for(int x = 1; x <= getItemCount(); x++){
            transTotal = transTotal + Double.parseDouble(getDetail(x, "sNewAmtxx").toString());
        }
        
        return transTotal;
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
    
    public int getItemCount() throws SQLException{
        p_oDetail.last();
        return p_oDetail.getRow();
    }
    
    public Object getDetail(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getItemCount() == 0 || fnRow > getItemCount()) return null;
        
        p_oDetail.absolute(fnRow);
        switch (fnIndex){
            case 5://nTotalAmt
            case 6:
                return DecryptAmount(p_oDetail.getString(fnIndex));
            default:
                return p_oDetail.getObject(fnIndex);
        }
    }
    public Object getDetail(int fnRow, String fsIndex) throws SQLException{
        return getDetail(fnRow, getColumnIndex(p_oDetail, fsIndex));
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
    public Object getDepartment(int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oDeparment.first();
        return p_oDeparment.getObject(fnIndex);
    }
    
    public Object getDepartment(String fsIndex) throws SQLException{
        return getDepartment(getColumnIndex(p_oDeparment, fsIndex));
    }
    public void setBranch(){
        p_oDeparment = null;
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
                System.out.println("sDeptIDxx = " +(String) loJSON.get("sDeptIDxx"));
                System.out.println("sDeptName = " +(String) loJSON.get("sDeptName"));
                p_oMaster.updateString("sDeptIDxx", (String) loJSON.get("sDeptIDxx"));
                p_oMaster.updateString("xDeptName", (String) loJSON.get("sDeptName"));
                p_oMaster.updateRow();
                
                //recreate detail and other tables
                createDetail();

                if (p_oListener != null) p_oListener.MasterRetreive(17, getMaster(fnRow,"xDeptName"));
                
                return true;
            }
            
            p_oMaster.updateString("sDeptIDxx", "");
            p_oMaster.updateString("xDeptName", "");
            p_oMaster.updateRow();
                        
            //recreate detail and other tables
            createDetail();
            
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
        
        return true;
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
   
    public String getMessage(){
        return p_sMessage;
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
        
        p_oDetail = new CachedRowSetImpl();
        p_oDetail.setMetaData(meta);        
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
        
    
    }
    
    private double DecryptAmount(String fsValue){
        if (GSec.isIncAuthMaster(p_oApp.getEmployeeNo()))
            return Double.valueOf(MySQLAESCrypt.Decrypt(fsValue, p_oApp.SIGNATURE));
        else
            return (double) 0.00;
    }
    
    private String EncryptAmount(double fnValue){
        return MySQLAESCrypt.Encrypt(String.valueOf(fnValue), p_oApp.SIGNATURE);
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
         String lsSQL = "";
        String lsStat = String.valueOf(p_nTranStat);
        
        if (lsStat.length() > 1){
            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
                lsSQL += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            
            lsSQL = " g.cTranStat IN (" + lsSQL.substring(2) + ")";
        } else{            
            lsSQL = " g.cTranStat = " + SQLUtil.toSQL(lsStat);
        }
        lsSQL = " SELECT  " +
            "  IFNULL(a.sTransNox,'') sTransNox  " +
            ", IFNULL(a.nEntryNox,'') nEntryNox  " +
            ", IFNULL(a.sEmployID,'') sEmployID  " +
            ", IFNULL(a.dLastUpdt,'') dLastUpdt  " +
            ", IFNULL(a.sOldAmtxx,0) sOldAmtxx  " +
            ", IFNULL(a.sNewAmtxx,0) sNewAmtxx  " +
            ", IFNULL(a.sRemarksx,'') sRemarksx  " +
            ", IFNULL(c.sCompnyNm,'') xEmployNm   " +
            ", IFNULL(e.sPositnNm, '') xPositnNm   " +
            ", IFNULL(d.sBankAcct, '') xBankAcct  " +
            ", IFNULL(f.sBankName, '') xBankName   " +
            ", IFNULL(g.dEffctive, '') dEffctive   " +
            ", IFNULL(h.sDeptName, '') xDeptName" +
            " FROM Department_Incentive_Detail a  " +
            ", Employee_Master001 b   " +
            "     LEFT JOIN Client_Master c ON b.sEmployID = c.sClientID  " +
            "     LEFT JOIN `Position` e ON b.sPositnID = e.sPositnID  " +
            "     LEFT JOIN Employee_Incentive_Bank_Info d ON b.sEmployID = d.sEmployID  " +
            "     LEFT JOIN `Banks` f ON d.sBankIDxx = f.sBankIDxx , " +
            "     Department_Incentive_Master g " +
            "     LEFT JOIN Department h ON g.sDeptIDxx = h.sDeptIDxx" +
            "     LEFT JOIN Incentive i ON g.sInctveCD = i.sInctveCD" +
            " WHERE a.sEmployID = b.sEmployID   " +
            "    AND a.sTransNox = g.sTransNox ";
        return lsSQL;
    }
    private String getSQ_MasterDetail(){
        return "SELECT" +
                    "  IFNULL(a.sTransNox, '') sTransNox" +
                    ", IFNULL(a.nEntryNox, '') nEntryNox" +
                    ", IFNULL(a.sEmployID, '') sEmployID" +
                    ", IFNULL(a.sOldAmtxx, '') sOldAmtxx" +
                    ", IFNULL(a.sNewAmtxx, '') sNewAmtxx" +
                    ", IFNULL(c.sCompnyNm, '') xEmployNm" +
                    ", IFNULL(e.sPositnNm, '') xPositnNm" +
                    ", IFNULL(e.sPositnNm, '') xPositnNm   " +
                    ", IFNULL(d.sBankAcct, '') xBankAcct  " +
                    ", IFNULL(f.sBankName, '') xBankName   " +
                " FROM department_incentive_detail a" +
                    ", Employee_Master001 b" +
                        " LEFT JOIN Client_Master c ON b.sEmployID = c.sClientID" +
                        " LEFT JOIN `Position` e ON b.sPositnID = e.sPositnID" +
                        " LEFT JOIN Employee_Incentive_Bank_Info d ON b.sEmployID = d.sEmployID  " +
                        " LEFT JOIN `Banks` f ON d.sBankIDxx = f.sBankIDxx  " +
                " WHERE a.sEmployID = b.sEmployID" +
                " ORDER BY nEntryNox";
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
        if (p_oApp.getDepartment().equals("026"))
            System.setProperty(DEBUG_MODE, "1"); 
        else
            System.setProperty(DEBUG_MODE, "0"); 
        
        GSec.AuthIncEntry(p_oApp);
        GSec.AuthIncMaster(p_oApp); 
    }
}
