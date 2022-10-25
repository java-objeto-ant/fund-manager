/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.fund.manager.base;

import com.sun.rowset.CachedRowSetImpl;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.GSec;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.MySQLAESCrypt;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.constants.UserRight;

/**
 *
 * @author User
 */
public class DeptIncentiveRelease {
    private final String DEBUG_MODE = "app.debug.mode";
    private final String MASTER_TABLE = "Incentive_Releasing_Master";

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
        if (p_oApp.getDepartment().equals("026"))
            System.setProperty(DEBUG_MODE, "1"); 
        else
            System.setProperty(DEBUG_MODE, "0"); 
        
        GSec.AuthIncEntry(p_oApp);
        GSec.AuthIncMaster(p_oApp);
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
        
    }
    
    private void createMaster() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(15);

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
        
        meta.setColumnName(15, "sBatchNox");
        meta.setColumnLabel(15, "sBatchNox");
        meta.setColumnType(15, Types.VARCHAR);
        meta.setColumnDisplaySize(15, 12);
        
        p_oDeptMaster = new CachedRowSetImpl();
        p_oDeptMaster.setMetaData(meta);
        
        
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
    private String getSQ_Master(){
        String lsSQL = "";
        String lsStat = String.valueOf(p_nTranStat);
        
        if (lsStat.length() > 1){
            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
                lsSQL += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            
            lsSQL = "cTranStat IN (" + lsSQL.substring(2) + ")";
        } else{            
            lsSQL = "cTranStat = " + SQLUtil.toSQL(lsStat);
        }
        
        return MiscUtil.addCondition(
                "SELECT" +
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
                " FROM Incentive_Releasing_Master", lsSQL);
    }
    public String getSQ_DeptMaster(){
        return "SELECT" + 
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
                    ", IFNULL(a.sBatchNox, '') sBatchNox" +
                " FROM Department_Incentive_Master a" +
                        " LEFT JOIN Department b ON a.sDeptIDxx = b.sDeptIDxx" +
                        " LEFT JOIN Incentive d ON a.sInctveCD = d.sInctveCD" +
                    ", Branch c " +
                " WHERE LEFT(a.sTransNox, 4) = c.sBranchCd";
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
        p_oDeptDetail.last();
        return p_oDeptDetail.getRow();
//        return p_oDetail.size();
    }
    
    public Object getDeptDetail(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        if (getDetailItemCount() == 0 || fnRow > getDetailItemCount()) return null;
        
        p_oDeptDetail.absolute(fnRow);
       
        switch (fnIndex){
            case 5://sOldAmtxx
            case 6://sNewAmtxx
                return DecryptAmount((String) p_oDeptDetail.getObject(fnIndex));
            default:
                return p_oDeptDetail.getObject(fnIndex);
        }

//        return p_oDetail.get(fnRow);
    }
    
    private double DecryptAmount(String fsValue){
        return Double.valueOf(MySQLAESCrypt.Decrypt(fsValue, p_oApp.SIGNATURE));
    }
    
    private String EncryptAmount(double fnValue){
        return MySQLAESCrypt.Encrypt(String.valueOf(fnValue), p_oApp.SIGNATURE);
    }
    
    public Object getDeptDetail(int fnRow, String fsIndex) throws SQLException{
        return getDeptDetail(fnRow, getColumnIndex(p_oDeptDetail, fsIndex));
    }
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
        return getMaster(getColumnIndex(p_oMaster,fsIndex));
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

        p_sMessage = "";
        
        if (!GSec.isIncAuthMaster(p_oApp.getEmployeeNo())){
            p_sMessage = "Your account is not authorized to use this feature.";
            return false;
        } 
        
        initMaster();
        createMaster(); 
        createDetail();
        p_oDeptMaster.last();
        p_oDeptMaster.moveToInsertRow();
        
       
        MiscUtil.initRowSet(p_oDeptMaster);     
        String lsSQL= getSQ_DeptMaster();
       
        p_oTag = new ArrayList<>();
        
        lsSQL = MiscUtil.addCondition(lsSQL, " TRIM(a.sBatchNox) IS NULL");
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
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
            p_oDeptMaster.updateString("sBatchNox", loRS.getString("sBatchNox"));
            p_oTag.add("0");
            p_oDeptMaster.insertRow();
            p_oDeptMaster.moveToCurrentRow();
           
            lnRow++;
        }
        
        p_nEditMode = EditMode.ADDNEW;
        return true;
    }
    public boolean SearchTransaction(String fsValue, boolean fbByCode) throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        if (!GSec.isIncAuthMaster(p_oApp.getEmployeeNo())){
            p_sMessage = "Your account is not authorized to use this feature.";
            return false;
        }        
        
        String lsSQL = getSQ_Master();
        String lsCondition;
        
        lsCondition = "sTransNox LIKE " + SQLUtil.toSQL(fsValue + "%");
        
        if (!lsSQL.isEmpty()) lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        
        if (p_bWithUI){
            JSONObject loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "Trans. No.»Date»Amount", 
                                "sTransNox»dTransact»nTranTotl", 
                                "sTransNox»dTransact»nTranTotl", 
                                fbByCode ? 0 : 1);
            
            if (loJSON != null) 
                return OpenTransaction((String) loJSON.get("sTransNox"));
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox = " + SQLUtil.toSQL(fsValue));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox LIKE " + SQLUtil.toSQL(fsValue + "%")); 
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
        
        if (!GSec.isIncAuthMaster(p_oApp.getEmployeeNo())){
            p_sMessage = "Your account is not authorized to use this feature.";
            return false;
        } 
        
        createMaster(); 
        createDetail();
        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        
        //open master
        lsSQL = MiscUtil.addCondition(getSQ_Master(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oMaster = factory.createCachedRowSet();
        p_oMaster.populate(loRS);
        MiscUtil.close(loRS);
        
        if (p_oMaster.size() == 0){
            p_sMessage = "No transaction to open.";
            return false;
        }
     
        //open dept master
       
        lsSQL = MiscUtil.addCondition(getSQ_DeptMaster(), "a.sBatchNox = " + SQLUtil.toSQL(fsTransNox));
        
        p_oTag = new ArrayList<>();
        
        
        loRS = p_oApp.executeQuery(lsSQL);
       
        p_oTag = new ArrayList<>();
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
            p_oDeptMaster.updateString("sBatchNox", loRS.getString("sBatchNox"));
            p_oTag.add("1");
            p_oDeptMaster.insertRow();
            p_oDeptMaster.moveToCurrentRow();
            
            if (OpenDeptTransaction(loRS.getString("sTransNox"))){   
            }
            lnRow++;
        }
       
        
        MiscUtil.close(loRS);
        p_nEditMode = EditMode.READY;
        
        return true;
    }
    
    public boolean OpenDeptTransaction(String fsTransNox) throws SQLException{
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
        
        //open detail
        lsSQL = MiscUtil.addCondition(getSQ_DeptDetail(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDeptDetail = factory.createCachedRowSet();
        p_oDeptDetail.populate(loRS);
        MiscUtil.close(loRS);
        
        return true;
    }
   
    public boolean SaveTransaction() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        if (p_nEditMode != EditMode.ADDNEW){
            p_sMessage = "Invalid edit mode detected.";
            return false;
        }
        
        if (!isEntryOK()){
            p_sMessage = "No record was tagged for release.";
            return false;
        }
        
        String lsTransNox = MiscUtil.getNextCode(MASTER_TABLE, "sTransNox", true, p_oApp.getConnection(), p_sBranchCd);
        
        if (!p_bWithParent) p_oApp.beginTrans();
        
        String lsSQL;
        int lnCtr, lnCtr2, lnCtr3 = 0;
        double lnTranTotl = 0.00;
        
        for (lnCtr = 0; lnCtr <= p_oTag.size()-1; lnCtr++){
            if (p_oTag.get(lnCtr).equals("1")){
                lsSQL = "UPDATE Department_Incentive_Master SET" +
                            "  sBatchNox = " + SQLUtil.toSQL(lsTransNox) +
                        " WHERE sTransNox = " + SQLUtil.toSQL((String) getDeptMaster(lnCtr + 1,"sTransNox"));
        
                if (p_oApp.executeQuery(lsSQL, "Department_Incentive_Master", p_sBranchCd, ((String)getDeptMaster(lnCtr + 1,"sTransNox")).substring(0, 4)) <= 0){
                    if (!p_bWithParent) p_oApp.rollbackTrans();
                    p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                    return false;
                }
                
                for (lnCtr2 = 1; lnCtr2 <= getDetailItemCount(); lnCtr2++){
                    lnTranTotl += Double.parseDouble(getDeptDetail(lnCtr2, "sNewAmtxx").toString());
                }
                
                lnCtr3 += 1;
            }
        }
        
        p_oMaster.first();
        p_oMaster.updateObject("sTransNox", lsTransNox);
        p_oMaster.updateObject("nTranTotl", lnTranTotl);
        p_oMaster.updateObject("nEntryNox", lnCtr3);
        p_oMaster.updateObject("sModified", p_oApp.getUserID());
        p_oMaster.updateObject("dModified", p_oApp.getServerDate());        
        
        lsSQL = MiscUtil.rowset2SQL(p_oMaster, MASTER_TABLE, "");
        
        if (p_oApp.executeQuery(lsSQL, "MASTER_TABLE", p_sBranchCd, "") <= 0){
            if (!p_bWithParent) p_oApp.rollbackTrans();
            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
            return false;
        }
        
        if (!p_bWithParent) p_oApp.commitTrans();
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public boolean ConfirmTransaction() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid update mode detected.";
            return false;
        }
        
        p_sMessage = "";
        
        if (!GSec.isIncAuthMaster(p_oApp.getEmployeeNo())){
            p_sMessage = "Your account is not authorized to use this feature.";
            return false;
        } 
        
        if (p_bWithParent) {
            p_sMessage = "Approving transactions from other object is not allowed.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("1")){
            p_sMessage = "Transaction was already confirmed.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("2")){
            p_sMessage = "Unable to confirm already posted transactions.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("3")){
            p_sMessage = "Unable to confirm already cancelled transactions.";
            return false;
        }
        
        String lsSQL;
        lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                    "  cTranStat = '1'" +
                    ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                    ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                " WHERE sTransNox = " + SQLUtil.toSQL((String) getMaster("sTransNox"));
        
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, ((String) getMaster("sTransNox")).substring(0, 4)) <= 0){
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }
        
        if (p_oListener != null) p_oListener.MasterRetreive(8, "1");
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    public boolean ReleaseTransaction() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid update mode detected.";
            return false;
        }
        
        p_sMessage = "";
        
        if (!GSec.isIncAuthMaster(p_oApp.getEmployeeNo())){
            p_sMessage = "Your account is not authorized to use this feature.";
            return false;
        } 
        
        if (p_bWithParent) {
            p_sMessage = "Approving transactions from other object is not allowed.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("0")){
            p_sMessage = "Unable to release unapproved transactions.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("2")){
            p_sMessage = "Unable to release already posted transactions.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("3")){
            p_sMessage = "Unable to release cancelled transactions.";
            return false;
        }
        
        //export with bank
        if (!exportWBank()) return false;
        
        //export no bank
        if (!exportNBank()) return false;
        
        String lsSQL;
        lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                    "  cTranStat = '2'" +
                    ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                    ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                " WHERE sTransNox = " + SQLUtil.toSQL((String) getMaster("sTransNox"));
        
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, ((String) getMaster("sTransNox")).substring(0, 4)) <= 0){
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }
        
        if (p_oListener != null) p_oListener.MasterRetreive(8, "1");
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    private boolean exportWBank(){
        try {
            p_oDeptMaster.beforeFirst();
            
            String dir = System.getProperty("sys.default.path.config") + "/temp/";
            
            File path = new File(dir + "BDO ATM Payroll Converter.XLS");
            FileInputStream file = new FileInputStream(path);

            Workbook workbook = new HSSFWorkbook(new POIFSFileSystem(file), true);
            Sheet sheet = workbook.getSheetAt(0);
            file.close();
            
            Cell cell;
            Row row;
            
            int lnRow = 5;
            
            while (p_oDeptMaster.next()){
                if (OpenDeptTransaction(p_oDeptMaster.getString("sTransNox"))){   
                    p_oDeptDetail.beforeFirst();
                    while(p_oDeptDetail.next()){
                        if (!p_oDeptDetail.getString("xBankAcct").isEmpty() &&
                            DecryptAmount(p_oDeptDetail.getString("sNewAmtxx")) > 0.00){
                            
                            lnRow ++;
                            row = sheet.createRow(lnRow);
                            
                            cell = row.createCell(0);
                            cell.setCellValue(p_oDeptDetail.getString("xBankAcct"));
                            
                            cell = row.createCell(1);
                            cell.setCellValue(DecryptAmount(p_oDeptDetail.getString("sNewAmtxx")));
                            
                            cell = row.createCell(2);
                            cell.setCellValue(p_oDeptDetail.getString("xEmployNm"));
                        }
                    }
                }
            }
            
            FileOutputStream outputFile= new FileOutputStream(dir + "incentive/BDO ATM Payroll Converter - " + p_oMaster.getString("sTransNox") + ".XLS");
            workbook.write(outputFile);
            outputFile.close();
            workbook.close();
        } catch (SQLException | FileNotFoundException | EncryptedDocumentException e) {
            p_sMessage = e.getMessage();
            e.printStackTrace();
            return false;
        } catch (IOException ex) {
            p_sMessage = ex.getMessage();
            ex.printStackTrace();
            return false;
        }
        
        return true;
    }    
    
    private boolean exportNBank(){
        try {
            p_oDeptMaster.beforeFirst();
            
            String dir = System.getProperty("sys.default.path.config") + "/temp/";
            
            File path = new File(dir + "No Bank.xls");
            FileInputStream file = new FileInputStream(path);

            Workbook workbook = WorkbookFactory.create(new File(System.getProperty("sys.default.path.config") + "/temp/No Bank.xls"));
            Sheet sheet = workbook.getSheetAt(0);
            file.close();
            
            Cell cell;
            Row row;
            
            int lnRow = 0;
            
            while (p_oDeptMaster.next()){
                if (OpenDeptTransaction(p_oDeptMaster.getString("sTransNox"))){   
                    p_oDeptDetail.beforeFirst();
                    while(p_oDeptDetail.next()){
                        if (p_oDeptDetail.getString("xBankAcct").isEmpty() ||
                            DecryptAmount(p_oDeptDetail.getString("sNewAmtxx")) <= 0.00){
                            
                            lnRow ++;
                            row = sheet.createRow(lnRow);
                            
                            cell = row.createCell(0);
                            cell.setCellValue(p_oDeptDetail.getString("xBankAcct"));
                            
                            cell = row.createCell(1);
                            cell.setCellValue(DecryptAmount(p_oDeptDetail.getString("sNewAmtxx")));
                            
                            cell = row.createCell(2);
                            cell.setCellValue(p_oDeptDetail.getString("xEmployNm"));
                        }
                    }
                }
            }
            
            FileOutputStream outputStream = new FileOutputStream(dir + "incentive/No Bank - " + p_oMaster.getString("sTransNox") + ".xls");
            workbook.write(outputStream);
            outputStream.close();
            workbook.close();
        } catch (SQLException | FileNotFoundException e) {
            p_sMessage = e.getMessage();
            e.printStackTrace();
            return false;
        } catch (IOException ex) {
            p_sMessage = ex.getMessage();
            ex.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    public boolean CancelTransaction() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid update mode detected.";
            return false;
        }
        
        p_sMessage = "";
        
        if (!GSec.isIncAuthMaster(p_oApp.getEmployeeNo())){
            p_sMessage = "Your account is not authorized to use this feature.";
            return false;
        } 
        
        if (p_bWithParent) {
            p_sMessage = "Approving transactions from other object is not allowed.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("3")){
            p_sMessage = "Transaction was already cancelled.";
            return false;
        }
        
        if (((String) getMaster("cTranStat")).equals("1")){
            p_sMessage = "Unable to cancel already closed transaction.";
            return false;
        }
        
        String lsSQL;
        lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                    "  cTranStat = '3'" +
                    ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                    ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                " WHERE sTransNox = " + SQLUtil.toSQL((String) getMaster("sTransNox"));
        
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, ((String) getMaster("sTransNox")).substring(0, 4)) <= 0){
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }
        
        if (p_oListener != null) p_oListener.MasterRetreive(8, "3");
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }
    
    private boolean isEntryOK(){
        boolean lbTag = false;
        
        for (int lnCtr = 0; lnCtr <= p_oTag.size()-1; lnCtr++){
            if (p_oTag.get(lnCtr).equals("1")){
                lbTag = true;
                break;
            }
        }
        
        return lbTag;
    }
    
}
