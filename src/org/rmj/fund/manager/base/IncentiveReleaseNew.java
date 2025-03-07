package org.rmj.fund.manager.base;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.constants.UserRight;
import org.rmj.fund.manager.parameters.IncentiveBankInfo;

/**
 * @author Valencia Maynard
 * @since 09-05-2024
 */
public class IncentiveReleaseNew {

    private final String FINANCE = "028";
    private final String MIS = "026";
    private final String DEBUG_MODE = "app.debug.mode";
    private final String REQUIRE_CSS = "app.require.css.approval";
    private final String REQUIRE_CM = "app.require.cm.approval";
    private final String MASTER_TABLE = "Incentive_Releasing_Master";

    private final GRider p_oApp;
    private final boolean p_bWithParent;

    private final Incentive p_oIncentive;
    private final IncentiveBankInfo p_oBankInfo;

    private CachedRowSet p_oDivision;

    private String p_sBranchCd;
    private int p_nEditMode;
    private int p_nTranStat;

    private String p_sMessage;
    private boolean p_bWithUI = true;

    private CachedRowSet p_oMaster;
    private CachedRowSet p_oDetail;
    private LMasDetTrans p_oListener;

    public IncentiveReleaseNew(GRider foApp, String fsBranchCd, boolean fbWithParent) {
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;

        if (p_sBranchCd.isEmpty()) {
            p_sBranchCd = p_oApp.getBranchCode();
        }

        p_oIncentive = new Incentive(p_oApp, p_sBranchCd, true);
        p_oIncentive.setTranStat(1);

        p_oBankInfo = new IncentiveBankInfo(p_oApp, p_sBranchCd, true);
        p_oBankInfo.setRecordStat(1);

        loadConfig();
        p_nTranStat = 0;
        p_nEditMode = EditMode.UNKNOWN;
    }

    public Object getDivision(int fnIndex) throws SQLException {
        if (fnIndex == 0) {
            return null;
        }

        p_oDivision.first();
        return p_oDivision.getObject(fnIndex);
    }

    public Object getDivision(String fsIndex) throws SQLException {
        return getDivision(getColumnIndex(p_oDivision, fsIndex));
    }

    public void setDivision() {
        p_oDivision = null;
    }

    public CachedRowSet getDivision() {
        return p_oDivision;
    }

    public void setTranStat(int fnValue) {
        p_nTranStat = fnValue;
    }

    public void setListener(LMasDetTrans foValue) {
        p_oListener = foValue;
    }

    public void setWithUI(boolean fbValue) {
        p_bWithUI = fbValue;
    }

    public int getEditMode() {
        return p_nEditMode;
    }

    public String getMessage() {
        return p_sMessage;
    }

    public int getItemCount() {
        return p_oDetail.size();
    }

    public IncentiveBankInfo getBankInfo(String fsEmployID) throws SQLException {
        if (p_oBankInfo.OpenRecord(fsEmployID)) {
            return p_oBankInfo;
        } else {
            return null;
        }
    }

    public Object getDetail(int fnRow, int fnIndex) throws SQLException {
        if (fnIndex == 0) {
            return null;
        }
        if (getItemCount() == 0 || fnRow > getItemCount()) {
            return null;
        }

        p_oDetail.absolute(fnRow);

        return p_oDetail.getObject(fnIndex);

    }

    public Object getDetail(int fnRow, String fsIndex) throws SQLException {
        return getDetail(fnRow, getColumnIndex(p_oDetail, fsIndex));
    }

    public Object getMaster(int fnIndex) throws SQLException {
        if (fnIndex == 0) {
            return null;
        }

        p_oMaster.first();
        return p_oMaster.getObject(fnIndex);
    }

    public Object getMaster(String fsIndex) throws SQLException {
        return getMaster(getColumnIndex(fsIndex));
    }

    public boolean NewTransaction(String fsPeriod) throws SQLException {
        String lsSQL;
        RowSetFactory factory = RowSetProvider.newFactory();
        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        if (p_oDivision == null) {
            p_sMessage = "No Selected Division for Retrieval.";
            return false;
        }
        if (fsPeriod.trim().isEmpty() || fsPeriod == null) {
            p_sMessage = "No specified Period for Retrieval.";
            return false;
        }
        if (!System.getProperty(DEBUG_MODE).equals("1")) {
            if (!p_oApp.getDepartment().equals(FINANCE)) {
                p_sMessage = "User is not allowed to use this application.";
                return false;
            }
        }

        p_sMessage = "";

        initMaster();
//        if (System.getProperty(REQUIRE_CSS).equals("1")) {
////            lsSQL = MiscUtil.addCondition(lsSQL, "a.cApprovd1 = '1'");
////        }
//
//        if (System.getProperty(REQUIRE_CM).equals("1")) {
//            lsSQL = MiscUtil.addCondition(lsSQL, "a.cApprovd2 = '1'");
//        }
//
//        //retreive only untagged entries
//        lsSQL = MiscUtil.addCondition(lsSQL, "sBatchNox = ''");
//
//        //add period 
//        lsSQL = MiscUtil.addCondition(lsSQL, " a.sMonthxxx = " + SQLUtil.toSQL(fsPeriod));
//        lsSQL = "SELECT * FROM (" + lsSQL + " ) IncentiveMaster "
//                + " LEFT JOIN Branch_Others d ON IncentiveMaster.xBranchCde = d.sBranchCD "
//                + " LEFT JOIN Division e ON d.cDivision = e.sDivsnCde ";
//        lsSQL = " SELECT "
//                + " a.sTransNox,"
//                + " a.dTransact,"
//                + " a.sDeptIDxx,"
//                + " a.sMonthxxx,"
//                + " a.sRemarksx,"
//                + " a.sPrepared,"
//                + " a.dPrepared,"
//                + " a.cApprovd1,"
//                + " a.sApprovd1,"
//                + " a.dApprovd1,"
//                + " a.cApprovd2,"
//                + " a.sApprovd2,"
//                + " a.dApprovd2,"
//                + " a.sBatchNox,"
//                + " a.cTranStat,"
//                + " COALESCE(c.sBranchNm, f.sBranchNm) AS xBranchNm,"
//                + " IFNULL (b.sDeptName, '')    xDeptName,"
//                + " COALESCE(a.sBranchCd, LEFT(a.sTransNox, 4)) AS xBranchCde"
//                + " FROM Incentive_Master a"
//                + " LEFT JOIN Department b ON a.sDeptIDxx = b.sDeptIDxx"
//                + " LEFT JOIN Branch c ON c.sBranchCd = a.sBranchCd"
//                + " LEFT JOIN Branch_Others d ON a.sBranchCd = d.sBranchCD"
//                + " LEFT JOIN Division e ON d.cDivision = e.sDivsnCde"
//                + " LEFT JOIN Branch f ON LEFT(a.sTransNox,4) = f.sBranchCd"
//                + " WHERE a.cTranStat = '1'"
//                + " AND a.cApprovd2 = '1'"
//                + " AND a.sBatchNox = '' ORDER BY xBranchCde";

        ResultSet loRS;
        //OpenDetail
        lsSQL = getSQ_Detail(fsPeriod, "");
        System.err.println("Retrieve Query = " + lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDetail = factory.createCachedRowSet();
        p_oDetail.populate(loRS);
        MiscUtil.close(loRS);
        p_nEditMode = EditMode.ADDNEW;

        if (getItemCount() == 0) {
            p_sMessage = "No incentive record to release.";
            return false;
        }
        return true;
    }

    public boolean SaveTransaction() throws SQLException {
        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";

        if (p_nEditMode != EditMode.ADDNEW) {
            p_sMessage = "Invalid edit mode detected.";
            return false;
        }

        if (!isEntryOK()) {
            p_sMessage = "No record was tagged for release.";
            return false;
        }

        String lsTransNox = MiscUtil.getNextCode(MASTER_TABLE, "sTransNox", true, p_oApp.getConnection(), p_sBranchCd);

        if (!p_bWithParent) {
            p_oApp.beginTrans();
        }

        String lsSQL;
        int lnCtr = 0;
        double lnTranTotl = 0.00;
        String lsDetailTransNox = "";

        Set<String> processedTransNox = new HashSet<>();
        for (lnCtr = 1; lnCtr <= p_oDetail.size(); lnCtr++) {
            p_oDetail.absolute(lnCtr);
            lnTranTotl += (double) p_oDetail.getDouble("xIncentve") - (double) p_oDetail.getDouble("xDeductnx");
            lsDetailTransNox = p_oDetail.getString("sTransNox");

            // Check if this transaction number has already been processed
            if (!processedTransNox.contains(lsDetailTransNox)) {
                lsSQL = "UPDATE Incentive_Master SET"
                        + "  sBatchNox = " + SQLUtil.toSQL(lsTransNox)
                        + " WHERE sTransNox = " + SQLUtil.toSQL(lsDetailTransNox);

                // Execute the query
                if (p_oApp.executeQuery(lsSQL, "Incentive_Master", p_sBranchCd, (lsDetailTransNox).substring(0, 4)) <= 0) {
                    if (!p_bWithParent) {
                        p_oApp.rollbackTrans();
                    }
                    p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                    return false;
                }

                // Add this transaction number to the processed set
                processedTransNox.add(lsDetailTransNox);
            }
        }

        p_oMaster.first();
        p_oMaster.updateObject("sTransNox", lsTransNox);
        p_oMaster.updateObject("nTranTotl", lnTranTotl);
        p_oMaster.updateObject("nEntryNox", lnCtr);
        p_oMaster.updateObject("sModified", p_oApp.getUserID());
        p_oMaster.updateObject("dModified", p_oApp.getServerDate());

        lsSQL = MiscUtil.rowset2SQL(p_oMaster, MASTER_TABLE, "");

        if (p_oApp.executeQuery(lsSQL, "MASTER_TABLE", p_sBranchCd, "") <= 0) {
            if (!p_bWithParent) {
                p_oApp.rollbackTrans();
            }
            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
            return false;
        }

        if (!p_bWithParent) {
            p_oApp.commitTrans();
        }

        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }

    public boolean SearchTransaction(String fsValue, boolean fbByCode) throws SQLException {
        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";

        if (System.getProperty(DEBUG_MODE).equals("0")) {
            if (Integer.valueOf(p_oApp.getEmployeeLevel()) < 1) {
                p_sMessage = "Your employee level is not authorized to use this transaction.";
                return false;
            }

            if (p_oApp.getUserLevel() < UserRight.SUPERVISOR) {
                p_sMessage = "Your account level is not authorized to use this transaction.";
                return false;
            }
        }

        String lsSQL = getSQ_Master();
//        String lsCondition = "";

//        lsCondition = "sTransNox LIKE " + SQLUtil.toSQL(p_oApp.getBranchCode() + "%");
//        lsCondition = "sTransNox LIKE " + SQLUtil.toSQL(%");
//        if (!lsSQL.isEmpty()) {
//            lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
//        }
        if (p_bWithUI) {
            JSONObject loJSON = showFXDialog.jsonSearch(
                    p_oApp,
                    lsSQL,
                    fsValue,
                    "Trans. No.»Date»Amount",
                    "sTransNox»dTransact»nTranTotl",
                    "sTransNox»dTransact»nTranTotl",
                    fbByCode ? 0 : 1);

            if (loJSON != null) {
                return OpenTransaction((String) loJSON.get("sTransNox"));
            } else {
                p_sMessage = "No record selected.";
                return false;
            }
        }

        if (fbByCode) {
            lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox = " + SQLUtil.toSQL(fsValue));
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox LIKE " + SQLUtil.toSQL(fsValue + "%"));

        }

        ResultSet loRS = p_oApp.executeQuery(lsSQL);

        if (!loRS.next()) {
            MiscUtil.close(loRS);
            p_sMessage = "No transaction found for the givern criteria.";
            return false;
        }

        lsSQL = loRS.getString("sTransNox");
        MiscUtil.close(loRS);

        return OpenTransaction(lsSQL);
    }

    public boolean OpenTransaction(String fsTransNox) throws SQLException {
        p_nEditMode = EditMode.UNKNOWN;

        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";

        if (System.getProperty(DEBUG_MODE).equals("0")) {
            if (Integer.valueOf(p_oApp.getEmployeeLevel()) < 1) {
                p_sMessage = "Your employee level is not authorized to use this transaction.";
                return false;
            }

            if (p_oApp.getUserLevel() < UserRight.SUPERVISOR) {
                p_sMessage = "Your account level is not authorized to use this transaction.";
                return false;
            }
        }

        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();

        //open master
        lsSQL = MiscUtil.addCondition(getSQ_Master(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        System.out.print(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oMaster = factory.createCachedRowSet();
        p_oMaster.populate(loRS);
        MiscUtil.close(loRS);

        if (p_oMaster.size() == 0) {
            p_sMessage = "No transaction to open.";
            return false;
        }

        lsSQL = getSQ_Detail("", fsTransNox);
        System.out.print(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDetail = factory.createCachedRowSet();
        p_oDetail.populate(loRS);
        MiscUtil.close(loRS);

        MiscUtil.close(loRS);
        p_nEditMode = EditMode.READY;

        return true;
    }

    public boolean ConfirmTransaction() throws SQLException {
        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        if (p_nEditMode != EditMode.READY) {
            p_sMessage = "Invalid update mode detected.";
            return false;
        }

        p_sMessage = "";

        if (p_bWithParent) {
            p_sMessage = "Approving transactions from other object is not allowed.";
            return false;
        }

        if (((String) getMaster("cTranStat")).equals("1")) {
            p_sMessage = "Transaction was already confirmed.";
            return false;
        }
        if (p_oApp.getUserLevel() < UserRight.SUPERVISOR) {
            p_sMessage = "Your account level is not authorized to use this transaction.";
            return false;
        }

        if (((String) getMaster("cTranStat")).equals("2")) {
            p_sMessage = "Unable to confirm already posted transactions.";
            return false;
        }

        if (((String) getMaster("cTranStat")).equals("3")) {
            p_sMessage = "Unable to confirm already cancelled transactions.";
            return false;
        }

        String lsSQL;
        lsSQL = "UPDATE " + MASTER_TABLE + " SET"
                + "  cTranStat = '1'"
                + ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID())
                + ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate())
                + " WHERE sTransNox = " + SQLUtil.toSQL((String) getMaster("sTransNox"));

        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, ((String) getMaster("sTransNox")).substring(0, 4)) <= 0) {
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }

        if (p_oListener != null) {
            p_oListener.MasterRetreive(8, "1");
        }

        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }

    public boolean CancelTransaction() throws SQLException {
        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        if (p_nEditMode != EditMode.READY) {
            p_sMessage = "Invalid update mode detected.";
            return false;
        }

        p_sMessage = "";

        if (p_bWithParent) {
            p_sMessage = "Approving transactions from other object is not allowed.";
            return false;
        }

        if (((String) getMaster("cTranStat")).equals("3")) {
            p_sMessage = "Transaction was already cancelled.";
            return false;
        }

        if (((String) getMaster("cTranStat")).equals("2")) {
            p_sMessage = "Unable to cancel already posted transaction.";
            return false;
        }

        String lsSQL;
        lsSQL = "UPDATE " + MASTER_TABLE + " SET"
                + "  cTranStat = '3'"
                + ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID())
                + ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate())
                + " WHERE sTransNox = " + SQLUtil.toSQL((String) getMaster("sTransNox"));

        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, ((String) getMaster("sTransNox")).substring(0, 4)) <= 0) {
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }

        if (p_oListener != null) {
            p_oListener.MasterRetreive(8, "3");
        }

        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }

    private boolean isEntryOK() {
        if (p_oDetail.size() == 0) {
            return false;
        }
        return true;
    }

    public void displayMasFields() throws SQLException {
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) {
            return;
        }

        int lnRow = p_oMaster.getMetaData().getColumnCount();

        System.out.println("----------------------------------------");
        System.out.println("MASTER TABLE INFO");
        System.out.println("----------------------------------------");
        System.out.println("Total number of columns: " + lnRow);
        System.out.println("----------------------------------------");

        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++) {
            System.out.println("Column index: " + (lnCtr) + " --> Label: " + p_oMaster.getMetaData().getColumnLabel(lnCtr));
            if (p_oMaster.getMetaData().getColumnType(lnCtr) == Types.CHAR
                    || p_oMaster.getMetaData().getColumnType(lnCtr) == Types.VARCHAR) {

                System.out.println("Column index: " + (lnCtr) + " --> Size: " + p_oMaster.getMetaData().getColumnDisplaySize(lnCtr));
            }
        }

        System.out.println("----------------------------------------");
        System.out.println("END: MASTER TABLE INFO");
        System.out.println("----------------------------------------");
    }

    private void loadConfig() {
        //update the value on configuration before deployment
        System.setProperty(DEBUG_MODE, "0");
        System.setProperty(REQUIRE_CSS, "0");
        System.setProperty(REQUIRE_CM, "1");
    }

    private void initMaster() throws SQLException {
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

    private String getSQ_Master() {
        return "SELECT"
                + "  sTransNox"
                + ", dTransact"
                + ", sApproved"
                + ", sPostedxx"
                + ", dPostedxx"
                + ", nTranTotl"
                + ", nEntryNox"
                + ", cTranStat"
                + ", sModified"
                + ", dModified"
                + " FROM Incentive_Releasing_Master";
    }

    private String getSQ_Detail(String fsMonth, String fsBatchNox) throws SQLException {
        String lsSQLIncentives = "SELECT "
                + " IFNULL (h.sAreaDesc, '') sAreaDesc"
                + ", IFNULL (e.sBranchNm, '') xBranchNm"
                + ", a.sTransNox "
                + ", a.sMonthxxx "
                + ", c.sEmployID "
                + ", IFNULL (f.sCompnyNm, '') xEmployNm "
                + ", IFNULL (j.sPositnNm, '') xPositnNm "
                + ", `gua_decrypt`(b.nInctvAmt, " + SQLUtil.toSQL(p_oApp.SIGNATURE) + ")"
                + " * (c.nAllcPerc / 100) + `gua_decrypt`(c.nAllcAmtx, " + SQLUtil.toSQL(p_oApp.SIGNATURE) + ") xIncentve"
                + ", '0.0' xDeductnx"
                + ", i.cRecdStat "
                + ", COALESCE(a.sBranchCd, LEFT(a.sTransNox, 4)) AS xBranchCde"
                + ", IFNULL (i.sBnkActNo, '') sBnkActNo"
                + ", IFNULL (i.sBankIDxx, '') sBankIDxx"
                + " FROM Incentive_Master a"
                + " LEFT JOIN Branch e ON COALESCE (a.sBranchCd, LEFT (a.sTransNox, 4))  = e.sBranchCd"
                + " LEFT JOIN Branch_Others g ON e.sBranchCd = g.sBranchCd"
                + " LEFT JOIN Branch_Area h ON g.sAreaCode = h.sAreaCode"
                + ", Incentive_Detail_Allocation b"
                + " LEFT JOIN Incentive d ON b.sInctveCD = d.sInctveCD"
                + ", Incentive_Detail_Allocation_Employee c"
                + " LEFT JOIN Client_Master f ON c.sEmployID = f.sClientID"
                + " LEFT JOIN Employee_Master001 i ON c.sEmployID = i.sEmployID"
                + " LEFT JOIN `Position` j ON i.sPositnID = j.sPositnID"
                + " WHERE a.sTransNox = b.sTransNox"
                + " AND b.sTransNox = c.sTransNox"
                + " AND b.sInctveCd = c.sInctveCd"
                + " AND a.cApprovd2 = '1' "
                + " AND a.cTranStat = '1'"
                + " AND a.sBatchNox = " + SQLUtil.toSQL(fsBatchNox);

        if (!fsMonth.isEmpty()) {
            lsSQLIncentives = MiscUtil.addCondition(lsSQLIncentives, " a.sMonthxxx = " + SQLUtil.toSQL(fsMonth));
        }

        if (p_oDivision != null) {
            lsSQLIncentives = MiscUtil.addCondition(lsSQLIncentives, " g.cDivision = " + SQLUtil.toSQL(getDivision("sDivsnCde")));
        }
        String lsSQLDeduction = " SELECT "
                + " IFNULL (h.sAreaDesc, '') sAreaDesc"
                + ", IFNULL (e.sBranchNm, '') xBranchNm"
                + ", a.sTransNox "
                + ", a.sMonthxxx"
                + ", c.sEmployID "
                + ", IFNULL (f.sCompnyNm, '') xEmployNm "
                + ", IFNULL (j.sPositnNm, '') xPositnNm "
                + ", '0.0' xIncentve"
                + ", CASE WHEN gua_decrypt(c.nAllcAmtx, " + SQLUtil.toSQL(p_oApp.SIGNATURE) + ")"
                + " THEN gua_decrypt(c.nAllcAmtx, " + SQLUtil.toSQL(p_oApp.SIGNATURE) + ") * (c.nAllcPerc / 100) "
                + " + gua_decrypt(c.nAllcAmtx, " + SQLUtil.toSQL(p_oApp.SIGNATURE) + ") "
                + " ELSE gua_decrypt(cc.nDedctAmt," + SQLUtil.toSQL(p_oApp.SIGNATURE) + ") * (c.nAllcPerc / 100)"
                + " END xDeductnx"
                + ", i.cRecdStat "
                + ", COALESCE(a.sBranchCd, LEFT(a.sTransNox, 4)) AS xBranchCde"
                + ", IFNULL (i.sBnkActNo, '') sBnkActNo"
                + ", IFNULL (i.sBankIDxx, '') sBankIDxx"
                + " FROM Incentive_Master a"
                + " LEFT JOIN Branch e ON COALESCE (a.sBranchCd, LEFT (a.sTransNox, 4))  = e.sBranchCd"
                + " LEFT JOIN Branch_Others g ON e.sBranchCd = g.sBranchCd"
                + " LEFT JOIN Branch_Area h ON g.sAreaCode = h.sAreaCode"
                + " LEFT JOIN Division k ON g.cDivision = k.sDivsnCde"
                + ", Incentive_Detail_Ded_Allocation_Employee c"
                + " LEFT JOIN Incentive_Detail_Ded_Allocation cc ON c.sTransNox = cc.sTransNox AND c.nEntryNox = cc.nEntryNox"
                + " LEFT JOIN Client_Master f ON c.sEmployID = f.sClientID"
                + " LEFT JOIN Employee_Master001 i ON c.sEmployID = i.sEmployID"
                + " LEFT JOIN `Position` j ON i.sPositnID = j.sPositnID"
                + " WHERE  a.sTransNox = c.sTransNox"
                + " AND a.cApprovd2 = '1' "
                + " AND a.sBatchNox = " + SQLUtil.toSQL(fsBatchNox)
                + " AND a.cTranStat = '1'"
                + " GROUP BY a.sTransNox, c.sEmployID, cc.nEntryNox";
        if (!fsMonth.isEmpty()) {
            lsSQLDeduction = MiscUtil.addCondition(lsSQLDeduction, " a.sMonthxxx = " + SQLUtil.toSQL(fsMonth));
        }

        if (p_oDivision != null) {
            lsSQLDeduction = MiscUtil.addCondition(lsSQLDeduction, " k.sDivsnCde = " + SQLUtil.toSQL(getDivision("sDivsnCde")));
        }
        return lsSQLIncentives + " UNION ALL " + lsSQLDeduction;
    }

    private int getColumnIndex(String fsValue) throws SQLException {
        int lnIndex = 0;
        int lnRow = p_oMaster.getMetaData().getColumnCount();

        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++) {
            if (fsValue.equals(p_oMaster.getMetaData().getColumnLabel(lnCtr))) {
                lnIndex = lnCtr;
                break;
            }
        }

        return lnIndex;
    }

    public String getSQ_Division() {
        String lsSQL = "";

        lsSQL = "SELECT"
                + "  sDivsnCde "
                + " , sDivsnDsc"
                + " FROM Division "
                + " WHERE cRecdStat = 1";

        return lsSQL;
    }

    public String getSQ_BranchOthers() {
        String lsSQL = "";

        lsSQL = "SELECT"
                + "  sBranchCD "
                + " , cDivision"
                + " FROM Branch_Others ";

        return lsSQL;
    }

    public boolean OpenDivision(String fsDivision) throws SQLException {
        p_nEditMode = EditMode.UNKNOWN;

        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";

        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();

        //open master
        lsSQL = MiscUtil.addCondition(getSQ_Division(), "sDivsnCde = " + SQLUtil.toSQL(fsDivision));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDivision = factory.createCachedRowSet();
        p_oDivision.populate(loRS);
        MiscUtil.close(loRS);

        p_nEditMode = EditMode.READY;

        return true;
    }

    public boolean getDivisionbyBranch(String fsBranch) throws SQLException {
        p_nEditMode = EditMode.UNKNOWN;

        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";

        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();

        lsSQL = MiscUtil.addCondition(getSQ_BranchOthers(), "sBranchCD = " + SQLUtil.toSQL(fsBranch));
        loRS = p_oApp.executeQuery(lsSQL);

        if (!loRS.next()) {
            MiscUtil.close(loRS);
            return false;
        }

        //open master
        lsSQL = MiscUtil.addCondition(getSQ_Division(), "sDivsnCde = " + SQLUtil.toSQL(loRS.getString("cDivision")));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDivision = factory.createCachedRowSet();
        p_oDivision.populate(loRS);
        MiscUtil.close(loRS);

        p_nEditMode = EditMode.READY;

        return true;
    }

    public boolean searchDivision(String fsValue, boolean fbByCode) throws SQLException {

        String lsSQL = getSQ_Division();
        if (fbByCode) {
            lsSQL = MiscUtil.addCondition(lsSQL, "sDivsnCde = " + SQLUtil.toSQL(fsValue));
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "sDivsnDsc LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }

        JSONObject loJSON;

        if (p_bWithUI) {
            loJSON = showFXDialog.jsonSearch(
                    p_oApp,
                    lsSQL,
                    fsValue,
                    "Code»Division Name",
                    "sDivsnCde»sDivsnDsc",
                    "sDivsnCde»sDivsnDsc",
                    fbByCode ? 0 : 1);

            if (loJSON != null) {
                return OpenDivision((String) loJSON.get("sDivsnCde"));
            } else {
                p_sMessage = "No record selected.";
                return false;
            }
        }

        if (fbByCode) {
            lsSQL = MiscUtil.addCondition(lsSQL, "sDivsnCde = " + SQLUtil.toSQL(fsValue));
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "sDivsnDsc LIKE " + SQLUtil.toSQL(fsValue + "%"));
            lsSQL += " LIMIT 1";
        }

        ResultSet loRS = p_oApp.executeQuery(lsSQL);

        if (!loRS.next()) {
            MiscUtil.close(loRS);
            p_sMessage = "No Division found for the given criteria.";
            return false;
        }

        String lsCode = loRS.getString("sDivsnCde");
        MiscUtil.close(loRS);

        return OpenDivision(lsCode);
    }

    private int getColumnIndex(CachedRowSet loRS, String fsValue) throws SQLException {
        int lnIndex = 0;
        int lnRow = loRS.getMetaData().getColumnCount();

        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++) {
            if (fsValue.equals(loRS.getMetaData().getColumnLabel(lnCtr))) {
                lnIndex = lnCtr;
                break;
            }
        }

        return lnIndex;
    }

    public boolean ReleaseTransaction() {
        try {
            if (p_oApp == null) {
                p_sMessage = "Application driver is not set.";
                return false;
            }

            if (p_nEditMode != EditMode.READY) {
                p_sMessage = "Invalid update mode detected.";
                return false;
            }

            p_sMessage = "";

            if (p_bWithParent) {
                p_sMessage = "Release transactions from other object is not allowed.";
                return false;
            }

            if (((String) getMaster("cTranStat")).equals("0")) {
                p_sMessage = "Transaction is not confirmed.";
                return false;
            }

            if (((String) getMaster("cTranStat")).equals("2")) {
                p_sMessage = "Unable to confirm already posted transactions.";
                return false;
            }

            if (((String) getMaster("cTranStat")).equals("3")) {
                p_sMessage = "Unable to confirm already cancelled transactions.";
                return false;
            }

            if (p_oApp.getUserLevel() < UserRight.SUPERVISOR) {
                p_sMessage = "Your account level is not authorized to use this transaction.";
                return false;
            }

            String lsSQL;
            lsSQL = "UPDATE " + MASTER_TABLE + " SET"
                    + "  cTranStat = '2'"
                    + ", sModified = " + SQLUtil.toSQL(p_oApp.getUserID())
                    + ", dModified = " + SQLUtil.toSQL(p_oApp.getServerDate())
                    + " WHERE sTransNox = " + SQLUtil.toSQL((String) getMaster("sTransNox"));

            if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, ((String) getMaster("sTransNox")).substring(0, 4)) <= 0) {
                p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
                return false;
            }

//            Set<String> processedTransNox = new HashSet<>(); // To track processed transaction numbers
//            String lsDetailTransNox;
//            for (int lnCtr = 1; lnCtr <= p_oDetail.size(); lnCtr++) {
//                p_oDetail.absolute(lnCtr);
//                lsDetailTransNox = p_oDetail.getString("sTransNox");
//
//                // Check if this transaction number has already been processed
//                if (!processedTransNox.contains(lsDetailTransNox)) {
//                    lsSQL = "UPDATE Incentive_Master SET"
//                            + "  cTranStat = " + SQLUtil.toSQL("7")
//                            + " WHERE sTransNox = " + SQLUtil.toSQL(lsDetailTransNox);
//
//                    // Execute the query
//                    if (p_oApp.executeQuery(lsSQL, "Incentive_Master", p_sBranchCd, (lsDetailTransNox).substring(0, 4)) <= 0) {
//                        if (!p_bWithParent) {
//                            p_oApp.rollbackTrans();
//                        }
//                        p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
//                        return false;
//                    }
//
//                    // Add this transaction number to the processed set
//                    processedTransNox.add(lsDetailTransNox);
//                }
//            }
            if (p_oListener != null) {
                p_oListener.MasterRetreive(8, "2");
            }
        } catch (SQLException ex) {
            Logger.getLogger(IncentiveReleaseNew.class.getName()).log(Level.SEVERE, null, ex);
            p_sMessage = ex.getMessage();
            return false;
        }
        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }

    public ResultSet getEmployeeDetail(String fsEmployID) throws SQLException {
        p_sMessage = "";

        String lsSQL;
        ResultSet loRS;

        //open master
        lsSQL = MiscUtil.addCondition(getSQ_Employee(), "a.sEmployID = " + SQLUtil.toSQL(fsEmployID));
        loRS = p_oApp.executeQuery(lsSQL);
        System.out.println (lsSQL);
        if (!loRS.next()) {
            MiscUtil.close(loRS);
            return null;
        }

        return loRS;

    }

    private String getSQ_Employee() {
        String lsSQL;

        lsSQL = " SELECT "
                + " a.sEmployID "
                + ", b.sLastName "
                + ", b.sFrstName "
                + ", IFNULL(b.sMiddName, '') sMiddName "
                + ", IFNULL(b.sMaidenNm, '') sMaidenNm "
                + ", IFNULL(b.sEmailAdd, '') sEmailAdd "
                + ", IFNULL(b.sMobileNo, '') sMobileNo "
                + ", IFNULL(c.sBankName, '') sBankName "
                + " FROM Employee_Master001 a "
                + " LEFT JOIN Client_Master b "
                + " ON a.sEmployID = b.sClientID "
                + " LEFT JOIN Banks c "
                + " ON a.sBankIDxx = c.sBankIDxx ";

        return lsSQL;
    }
}
