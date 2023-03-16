/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.fund.manager.parameters;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
import static org.rmj.appdriver.MiscUtil.rowset2SQL;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.fund.manager.base.LMasDetTrans;

/**
 *
 * @author User
 */
public class Raffle {
    private final String MASTER_TABLE = "ILMJ_Master";
    
    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    private String p_sBranchCd;
    private int p_nEditMode;

    private String p_sMessage;
    private boolean p_bWithUI = true;
    
    private LMasDetTrans p_oListener;
    
    private CachedRowSet p_oMaster;
    
    public Raffle(GRider foApp, String fsBranchCd, boolean fbWithParent){
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
        
        initMaster();
        System.out.println("sTransNox = " + getMaster("sTransNox").toString());
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
        
        p_oMaster.updateObject("sModified", p_oApp.getUserID());
        p_oMaster.updateObject("dModified", p_oApp.getServerDate());
        p_oMaster.updateRow();
        
        
        String lsSQL;
        
        if (p_nEditMode == EditMode.ADDNEW){           
            lsSQL = rowset2SQL(p_oMaster, MASTER_TABLE, "");
        } else {            
            lsSQL = rowset2SQL(p_oMaster, 
                                        MASTER_TABLE, 
                                        "", 
                                        "sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox")));
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
        } else{
            p_sMessage = "No record to update.";
        }
       
        
        
        p_nEditMode = EditMode.UNKNOWN;
        return false;
    }
    
    public boolean ActivateRecord() throws SQLException{
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid edit mode.";
            return false;
        }
        
        p_oMaster.first();
        
        if ("1".equals(p_oMaster.getString("cTranStat"))){
            p_sMessage = "Attendee is already present..";
            return false;
        }
        
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                            "  cTranStat = '1'" +
                            ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID()) +
                            ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate()) +
                        " WHERE sTransNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox"));

        if (!p_bWithParent) p_oApp.beginTrans();
        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, "") <= 0){
            if (!p_bWithParent) p_oApp.rollbackTrans();
            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
            return false;
        }
        if (!p_bWithParent) p_oApp.commitTrans();
        
        p_nEditMode = EditMode.UNKNOWN;
        return true;
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
        lsSQL = MiscUtil.addCondition(getSQ_Record(), "a.sAttndIDx = " + SQLUtil.toSQL(fsValue));
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oMaster = factory.createCachedRowSet();
        p_oMaster.populate(loRS);
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
   
    
    public int getItemCount() throws SQLException{
        if (p_oMaster == null) return 0;
        
        p_oMaster.last();
        return p_oMaster.getRow();
    }
     public Object getMaster(int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oMaster.first();
        return p_oMaster.getObject(fnIndex);
    }
    
    public Object getMaster(String fsIndex) throws SQLException{
        return getMaster(getColumnIndex(p_oMaster,fsIndex));
    }
    public Object getMaster(int fnRow, int fnIndex) throws SQLException{
        if (fnIndex == 0) return null;
        
        p_oMaster.absolute(fnRow);
        return p_oMaster.getObject(fnIndex);
    }
    
    public Object getMaster(int fnRow, String fsIndex) throws SQLException{
        return getMaster(fnRow, getColumnIndex(p_oMaster, fsIndex));
    }
    public void setMaster(String fsIndex, Object foValue) throws SQLException{        
        setMaster(getColumnIndex(p_oMaster,fsIndex), foValue);
    }
    public void setMaster(int fnIndex, Object foValue) throws SQLException{
        p_oMaster.first();
        switch (fnIndex){
            case 4://sFirstNme
            case 6://sSuffixNm
                p_oMaster.updateString(fnIndex, ((String) foValue).trim());
                p_oMaster.updateRow();

                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oMaster.getString(fnIndex));
                break;
            case 5://cTranStat
                
                if (foValue instanceof Integer)
                    p_oMaster.updateInt(fnIndex, (int) foValue);
                else 
                    p_oMaster.updateInt(fnIndex, 0);
                
                p_oMaster.updateRow();
                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oMaster.getString(fnIndex));
                break;
            case 2://dTransact
            case 3://dRaffleDt
            case 7://dModified
                if (foValue instanceof Date){
                    p_oMaster.updateDate(fnIndex, SQLUtil.toDate((Date) foValue));
                } else
                    p_oMaster.updateDate(fnIndex, SQLUtil.toDate(p_oApp.getServerDate()));
                
                p_oMaster.updateRow();
                
                if (p_oListener != null) p_oListener.MasterRetreive(fnIndex, p_oMaster.getString(fnIndex));
                break;
        }
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
    
    
    private boolean isEntryOK() throws SQLException{
        p_oMaster.first();
        
        if (p_oMaster.getString("dRaffleDt").isEmpty()){
            p_sMessage = "Raffle date must not be empty.";
            return false;
        }
        
        return true;
    }
    
    private void initMaster() throws SQLException{
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(8);

        meta.setColumnName(1, "sTransNox");
        meta.setColumnLabel(1, "sTransNox");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 12);
        
        meta.setColumnName(2, "dTransact");
        meta.setColumnLabel(2, "dTransact");
        meta.setColumnType(2, Types.DATE);
        
        meta.setColumnName(3, "dRaffleDt");
        meta.setColumnLabel(3, "dRaffleDt");
        meta.setColumnType(3, Types.DATE);
        
        meta.setColumnName(4, "sRemarksx");
        meta.setColumnLabel(4, "sRemarksx");
        meta.setColumnType(4, Types.VARCHAR);
        meta.setColumnDisplaySize(4, 128);
        
        meta.setColumnName(5, "cTranStat");
        meta.setColumnLabel(5, "cTranStat");
        meta.setColumnType(5, Types.VARCHAR);
        meta.setColumnDisplaySize(5, 1);
        
        meta.setColumnName(6, "sModified");
        meta.setColumnLabel(6, "sModified");
        meta.setColumnType(6, Types.VARCHAR);
        meta.setColumnDisplaySize(6, 12);
        
        meta.setColumnName(7, "dModified");
        meta.setColumnLabel(7, "dModified");
        meta.setColumnType(7, Types.DATE);
        
        meta.setColumnName(8, "dTimeStmp");
        meta.setColumnLabel(8, "dTimeStmp");
        meta.setColumnType(8, Types.TIMESTAMP);
        
        p_oMaster = new CachedRowSetImpl();
        p_oMaster.setMetaData(meta);
        
        p_oMaster.last();
        p_oMaster.moveToInsertRow();
        
        MiscUtil.initRowSet(p_oMaster);    
        
        p_oMaster.updateString("sTransNox", MiscUtil.getNextCode(MASTER_TABLE, "sTransNox", false, p_oApp.getConnection(), ""));
        p_oMaster.updateObject("cTranStat", 0);
        
        p_oMaster.insertRow();
        p_oMaster.moveToCurrentRow();
    }
        
    private String getSQ_Record(){
        return "SELECT" +
                    " IFNULL(a.sTransNox,'')  sTransNox" +
                    ", IFNULL(a.dTransact,'') dTransact" +
                    ", IFNULL(a.dRaffleDt,'') dRaffleDt" +
                    ", IFNULL(a.sRemarksx,'') sRemarksx" +
                    ", IFNULL(a.cTranStat, 0) cTranStat" +
                    ", IFNULL(a.sModified,'') sModified" +
                    ", IFNULL(a.dModified,'') dModified" +
                    ", IFNULL(a.dTimeStmp,'') dTimeStmp" +
                " FROM " + MASTER_TABLE ;
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
        System.out.println("----------------------------------------");
        System.out.println("FIELD VALUES");
        System.out.println("----------------------------------------");
        System.out.println("----------------------------------------");
    }
    
}
