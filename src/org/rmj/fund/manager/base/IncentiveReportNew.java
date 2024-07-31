package org.rmj.fund.manager.base;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.MySQLAESCrypt;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;

/**
 *
 * @author Maynard start 7-16-2024
 */
public class IncentiveReportNew {

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

    private CachedRowSet p_oRecord;
    private CachedRowSet p_oRecordProcessed;
    private CachedRowSet p_oBranch;
    private CachedRowSet p_oCategory;
    private CachedRowSet p_oDivision;
    private CachedRowSet p_oBranchArea;

    private String p_sBranchCd;

    private int p_nEditMode;
    private int p_nTranStat;

    private String p_sMessage;
    private boolean p_bWithUI = true;

    public IncentiveReportNew(GRider foApp, String fsBranchCd, boolean fbWithParent) {
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;

        if (p_sBranchCd.isEmpty()) {
            p_sBranchCd = p_oApp.getBranchCode();
        }

        loadConfig();

        p_nTranStat = 0;
        p_nEditMode = EditMode.UNKNOWN;
    }

    private void loadConfig() {
        //update the value on configuration before deployment
        System.setProperty(DEBUG_MODE, "0");
        System.setProperty(REQUIRE_CSS, "0");
        System.setProperty(REQUIRE_CM, "1");
        System.setProperty(REQUIRE_BANK_ON_APPROVAL, "0");
    }

    public void setTranStat(int fnValue) {
        p_nTranStat = fnValue;
    }

    public int getTranStats() {
        return p_nTranStat;
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

    public int getItemCount() throws SQLException {
        if (p_oRecordProcessed == null) {
            return 0;
        }
        p_oRecordProcessed.last();
        return p_oRecordProcessed.getRow();
    } 
    public int getSQLItemCount() throws SQLException {
        if (p_oRecord == null) {
            return 0;
        }
        p_oRecord.last();
        return p_oRecord.getRow();
    }

    public Object getRecord(int fnRow, int fnIndex) throws SQLException {
        if (fnIndex == 0) {
            return null;
        }
        if (getItemCount() == 0 || fnRow > getItemCount()) {
            return null;
        }

        p_oRecordProcessed.absolute(fnRow);
        return p_oRecordProcessed.getObject(fnIndex);

    }

    public Object getRecord(int fnRow, String fsIndex) throws SQLException {
        return getRecord(fnRow, getColumnIndex(p_oRecordProcessed, fsIndex));
    }

    public Object getFilter(int fiFilter, String fsIndex) throws SQLException {
        switch (fiFilter) {
            case 2:
                return getBranch(getColumnIndex(p_oBranch, fsIndex));
            case 3:
                return getDivision(getColumnIndex(p_oDivision, fsIndex));
            case 4:
                return getCategory(getColumnIndex(p_oCategory, fsIndex));
            case 5:
                return getBranchArea(getColumnIndex(p_oBranchArea, fsIndex));
        }
        return null;
    }

    public Object getBranch(int fnIndex) throws SQLException {
        if (fnIndex == 0) {
            return null;
        }

        p_oBranch.first();
        return p_oBranch.getObject(fnIndex);
    }

    public Object getBranch(String fsIndex) throws SQLException {
        return getBranch(getColumnIndex(p_oBranch, fsIndex));
    }

    public void setBranch() {
        p_oBranch = null;
    }

    public CachedRowSet getBranch() {
        return p_oBranch;
    }

    public Object getBranchArea(int fnIndex) throws SQLException {
        if (fnIndex == 0) {
            return null;
        }

        p_oBranchArea.first();
        return p_oBranchArea.getObject(fnIndex);
    }

    public Object getBranchArea(String fsIndex) throws SQLException {
        return getBranchArea(getColumnIndex(p_oBranchArea, fsIndex));
    }

    public void setBranchArea() {
        p_oBranchArea = null;
    }

    public CachedRowSet getBranchArea() {
        return p_oBranchArea;
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

    public Object getCategory(int fnIndex) throws SQLException {
        if (fnIndex == 0) {
            return null;
        }

        p_oCategory.first();
        return p_oCategory.getObject(fnIndex);
    }

    public Object getCategory(String fsIndex) throws SQLException {
        return getCategory(getColumnIndex(p_oCategory, fsIndex));
    }

    public void setCategory() {
        p_oCategory = null;
    }

    public CachedRowSet getCategory() {
        return p_oCategory;
    }

    private String getSQ_Record() {
        String lsSQL;

        lsSQL = " SELECT "
                + " sTransNox , "
                + " sMonthxxx , "
                + " dTransact , "
                + " IFNULL(sBranchCd,'') sBranchCd , "
                + " IFNULL(sBranchNm,'') sBranchNm , "
                + " IFNULL(sDivsnCde,'') sDivsnCde , "
                + " IFNULL(sDivsnDsc,'') sDivsnDsc , "
                + " IFNULL(sAreaCode,'') sAreaCode , "
                + " IFNULL(sAreaDesc,'') sAreaDesc , "
                + " sCompnyNm , "
                + " sEmployID , "
                + " sEmpLevID , "
                + " sEmpLevNm , "
                + " sPositnID , "
                + " sPositnNm , "
                + " IFNULL(sBankName,'') sBankName , "
                + " IFNULL(sBankAcct,'') sBankAcct , "
                + " xSrvcYear , "
                + " cRecdStat , "
                + " sInctveCD , "
                + " sInctveDs , "
                + " cTranStat , "
                + " nTotalAmt , "
                + " nAmtActlx , "
                + " nAmtGoalx , "
                + " nQtyActlx , "
                + " nQtyGoalx , "
                + " nInctvAmt , "
                + " nAllcPerc , "
                + " nAllcAmtx , "
                + " nDedctAmt , "
                + " xDedAlcPer , "
                + " xDedAlcAmt "
                + " FROM  ( SELECT "
                + " a.sTransNox sTransNox , "
                + " a.sMonthxxx sMonthxxx , "
                + " a.dTransact dTransact , "
                + " g.sBranchCd sBranchCd , "
                + " g.sBranchNm sBranchNm , "
                + " i.sDivsnCde sDivsnCde , "
                + " i.sDivsnDsc sDivsnDsc , "
                + " q.sAreaCode sAreaCode , "
                + " q.sAreaDesc sAreaDesc , "
                + " d.sCompnyNm sCompnyNm , "
                + " b.sEmployID sEmployID , "
                + " f.sEmpLevID sEmpLevID , "
                + " f.sEmpLevNm sEmpLevNm , "
                + " e.sPositnID sPositnID , "
                + " e.sPositnNm sPositnNm , "
                + " k.sBankName sBankName , "
                + " j.sBankAcct sBankAcct , "
                + " IFNULL(ROUND(DATEDIFF(NOW(), IFNULL(c.dStartEmp, c.dHiredxxx)) / 365), '') xSrvcYear , "
                + " c.cRecdStat cRecdStat , "
                + " n.sInctveCD sInctveCD , "
                + " n.sInctveDs sInctveDs , "
                + " a.cTranStat cTranStat , "
                + " b.nTotalAmt nTotalAmt , "
                + " m.nAmtActlx nAmtActlx , "
                + " m.nAmtGoalx nAmtGoalx , "
                + " m.nQtyActlx nQtyActlx , "
                + " m.nQtyGoalx nQtyGoalx , "
                + " IFNULL(m.nInctvAmt,'1B09259D73D06C95C457CB3A89B03299') nInctvAmt , "
                + " l.nAllcPerc nAllcPerc , "
                + " IFNULL(l.nAllcAmtx,'1B09259D73D06C95C457CB3A89B03299') nAllcAmtx , "
                + " '1B09259D73D06C95C457CB3A89B03299' nDedctAmt , "
                + " 0.0 xDedAlcPer , "
                + " '1B09259D73D06C95C457CB3A89B03299' xDedAlcAmt "
                + " FROM Incentive_Master a "
                + " JOIN Incentive_Detail b "
                + " ON a.sTransNox = b.sTransNox "
                + " LEFT JOIN Employee_Master001 c "
                + " ON b.sEmployID = c.sEmployID "
                + " LEFT JOIN Client_Master d "
                + " ON b.sEmployID = d.sClientID "
                + " LEFT JOIN Position e "
                + " ON c.sPositnID = e.sPositnID "
                + " LEFT JOIN Employee_Level f "
                + " ON c.sEmpLevID = f.sEmpLevID "
                + " LEFT JOIN Branch g "
                + " ON a.sBranchCd = g.sBranchCd "
                + " LEFT JOIN Branch_Others h "
                + " ON g.sBranchCd = h.sBranchCD "
                + " LEFT JOIN Branch_Area q "
                + " ON h.sAreaCode = q.sAreaCode "
                + " LEFT JOIN Division i "
                + " ON h.cDivision = i.sDivsnCde "
                + " LEFT JOIN Employee_Incentive_Bank_Info j "
                + " ON b.sEmployID = j.sEmployID "
                + " LEFT JOIN Banks k "
                + " ON j.sBankIDxx = k.sBankIDxx "
                + " LEFT JOIN Incentive_Detail_Allocation_Employee l "
                + " ON b.sTransNox  = l.sTransNox "
                + " AND b.sEmployID = l.sEmployID "
                + " LEFT JOIN Incentive_Detail_Allocation m "
                + " ON l.sTransNox = m.sTransNox "
                + " AND l.sInctveCD = m.sInctveCD "
                + " LEFT JOIN Incentive n "
                + " ON m.sInctveCD = n.sInctveCD WHERE l.sTransNox IS NOT NULL "
                + " UNION SELECT "
                + " a.sTransNox sTransNox , "
                + " a.sMonthxxx sMonthxxx , "
                + " a.dTransact dTransact , "
                + " g.sBranchCd sBranchCd , "
                + " g.sBranchNm sBranchNm , "
                + " i.sDivsnCde sDivsnCde , "
                + " i.sDivsnDsc sDivsnDsc , "
                + " q.sAreaCode sAreaCode , "
                + " q.sAreaDesc sAreaDesc , "
                + " d.sCompnyNm sCompnyNm , "
                + " b.sEmployID sEmployID , "
                + " f.sEmpLevID sEmpLevID , "
                + " f.sEmpLevNm sEmpLevNm , "
                + " e.sPositnID sPositnID , "
                + " e.sPositnNm sPositnNm , "
                + " k.sBankName sBankName , "
                + " j.sBankAcct sBankAcct , "
                + " IFNULL(ROUND(DATEDIFF(NOW(), IFNULL(c.dStartEmp, c.dHiredxxx)) / 365), '') xSrvcYear , "
                + " c.cRecdStat cRecdStat , "
                + " '999' sInctveCD , "
                + " 'Deduction' sInctveDs , "
                + " a.cTranStat cTranStat , "
                + " b.nTotalAmt nTotalAmt , "
                + " 0.0 nAmtActlx , "
                + " 0.0 nAmtGoalx , "
                + " 0.0 nQtyActlx , "
                + " 0.0 nQtyGoalx , "
                + " '1B09259D73D06C95C457CB3A89B03299' nInctvAmt , "
                + " 0.0 nAllcPerc , "
                + " '1B09259D73D06C95C457CB3A89B03299' nAllcAmtx , "
                + " IFNULL(o.nDedctAmt , '1B09259D73D06C95C457CB3A89B03299') nDedctAmt , "
                + " p.nAllcPerc xDedAlcPer , "
                + " IFNULL(p.nAllcAmtx , '1B09259D73D06C95C457CB3A89B03299') xDedAlcAmt "
                + " FROM Incentive_Master a  "
                + " LEFT JOIN Incentive_Detail b "
                + " ON a.sTransNox = b.sTransNox "
                + " LEFT JOIN Employee_Master001 c "
                + " ON b.sEmployID = c.sEmployID "
                + " LEFT JOIN Client_Master d "
                + " ON b.sEmployID = d.sClientID "
                + " LEFT JOIN Position e "
                + " ON c.sPositnID = e.sPositnID "
                + " LEFT JOIN Employee_Level f "
                + " ON c.sEmpLevID = f.sEmpLevID "
                + " LEFT JOIN Branch g "
                + " ON a.sBranchCd = g.sBranchCd "
                + " LEFT JOIN Branch_Others h "
                + " ON g.sBranchCd = h.sBranchCD "
                + " LEFT JOIN Branch_Area q "
                + " ON h.sAreaCode = q.sAreaCode "
                + " LEFT JOIN Division i "
                + " ON h.cDivision = i.sDivsnCde "
                + " LEFT JOIN Employee_Incentive_Bank_Info j "
                + " ON b.sEmployID = j.sEmployID "
                + " LEFT JOIN Banks k "
                + " ON j.sBankIDxx = k.sBankIDxx "
                + " LEFT JOIN Incentive_Detail_Ded_Allocation o "
                + " ON a.sTransNox = o.sTransNox "
                + " LEFT JOIN Incentive_Detail_Ded_Allocation_Employee p "
                + " ON a.sTransNox = p.sTransNox "
                + " AND b.sEmployID = p.sEmployID WHERE o.sTransNox IS NOT NULL ) Incentive";

//        lsSQL = MiscUtil.addCondition(lsSQL, lsCondition());
        return lsSQL;
    }

    public boolean OpenRecord(String fsValue, boolean isByBranch) throws SQLException {

        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }
        p_sMessage = "";
        String lsSQL = getSQ_Record();
        String lsCondition = "";
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        //period
        if (!fsValue.isEmpty()) {
            lsCondition = MiscUtil.addCondition(lsCondition, " sMonthxxx = " + SQLUtil.toSQL(fsValue));
        }
        //brnach
        if (p_oBranch != null) {
            lsCondition = MiscUtil.addCondition(lsCondition, " sBranchCD = " + SQLUtil.toSQL(getBranch("sBranchCd")));
        }
        //division
        if (p_oDivision != null) {
            lsCondition = MiscUtil.addCondition(lsCondition, "  sDivsnCde = " + SQLUtil.toSQL(getDivision("sDivsnCde")));
        }
        //incentive
        if (p_oCategory != null) {
            lsCondition = MiscUtil.addCondition(lsCondition, "  sInctveCD = " + SQLUtil.toSQL(getCategory("sInctveCD")));
        }
        //area
        if (p_oBranchArea != null) {
            lsCondition = MiscUtil.addCondition(lsCondition, "  sAreaCode = " + SQLUtil.toSQL(getBranchArea("sAreaCode")));
        }
        //ctranstat
        if (p_nTranStat >= 0) {
            lsCondition = MiscUtil.addCondition(lsCondition, "  cTranStat =  " + SQLUtil.toSQL(p_nTranStat));
        }

        lsCondition = lsCondition + getConfigFilter();
        if (!isByBranch) {
            lsSQL = lsSQL + lsCondition + " ORDER BY sCompnyNm, sTransNox, sInctveCD ";
        } else {
            lsSQL = lsSQL + lsCondition + " ORDER BY sBranchCD, sInctveCD ";
        }

        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oRecord = factory.createCachedRowSet();
        p_oRecord.populate(loRS);
        MiscUtil.close(loRS);

        loRS = p_oApp.executeQuery(getSQ_Record() + " WHERE 0=1");
        p_oRecordProcessed = factory.createCachedRowSet();
        p_oRecordProcessed.populate(loRS);
        MiscUtil.close(loRS);

        return DecryptIncentive();
    }

//    private String getTranStat() {
//        String lsStat = String.valueOf(p_nTranStat);
//        String lsCondition = "";
//        if (lsStat.length() >= 1) {
//            for (int lnCtr = 0; lnCtr <= lsStat.length() - 1; lnCtr++) {
//                lsCondition += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
//            }
//
//        }
//        return lsCondition;
//    }

    private String getConfigFilter() {
        String lsCondition = "";
        String lsStat = String.valueOf(p_nTranStat);

        System.out.println("department  = " + p_oApp.getDepartment());
        if (MAIN_OFFICE.contains(p_oApp.getBranchCode())) {
            if ((AUDITOR + "»" + COLLECTION + "»" + FINANCE).contains(p_oApp.getDepartment())) {
                if (p_oApp.getDepartment().equals(AUDITOR)) {
                    if (lsStat.equals("1")) {
                        lsCondition = lsCondition + " AND cApprovd2 = '0'";
                    } else if (lsStat.equals("2")) {
                        lsCondition = lsCondition + " AND cApprovd2 = '1'";
                    } else if (lsStat.equals("12")) {
                        lsCondition = lsCondition + " AND cApprovd2 IN ( '1' , '2' )";
                    }
                }
            } else {
                lsCondition = lsCondition + " AND LEFT(sTransNox,4) = " + SQLUtil.toSQL(p_oApp.getBranchCode());
            }
        } else {
            lsCondition = lsCondition + " AND LEFT(sTransNox,4)  = " + SQLUtil.toSQL(p_oApp.getBranchCode());

        }
        return lsCondition;
    }

    private double DecryptAmount(String fsValue) {
        return Double.valueOf(MySQLAESCrypt.Decrypt(fsValue, p_oApp.SIGNATURE));
    }

    private boolean DecryptIncentive() throws SQLException {
        if (p_oRecord == null) {
            p_sMessage = "No Record Found";
            return false;
        }

        if (getSQLItemCount() < 0) {
            p_sMessage = "No Record Found";
            return false;
        }
        System.err.println("Item to Decrypt = " + getSQLItemCount());
        System.err.println("System Start Decrypting Value's");
        try {
            p_oRecord.beforeFirst();
            while (p_oRecord.next()) {
                p_oRecord.updateObject("nTotalAmt", DecryptAmount(p_oRecord.getString("nTotalAmt")));
                p_oRecord.updateObject("nInctvAmt", DecryptAmount(p_oRecord.getString("nInctvAmt")));
                p_oRecord.updateObject("nAllcAmtx", DecryptAmount(p_oRecord.getString("nAllcAmtx")));
                p_oRecord.updateObject("nDedctAmt", DecryptAmount(p_oRecord.getString("nDedctAmt")));
                p_oRecord.updateObject("xDedAlcAmt", DecryptAmount(p_oRecord.getString("xDedAlcAmt")));

                p_oRecord.updateRow();
            }
        } catch (SQLException ex) {
            p_sMessage = ex.getMessage();
            return false;
        }
        p_nEditMode = EditMode.READY;

        System.err.println("System Finished Decrypting Value's");
        return true;
    }

    public boolean procReportSummarizedEmployee() {
        try {
            if (p_oRecord == null) {
                return false;
            }
            // Variables to track the current group and sums
            String currentEmployID = null;

            p_oRecordProcessed.beforeFirst();
            p_oRecord.beforeFirst();
            double xInctvAmt = 0;
            double xDedctAmt = 0;
            double xNetAmount = 0;

            while (p_oRecord.next()) {
                String employID = p_oRecord.getString("sEmployID");
                double inctvAmt = (p_oRecord.getDouble("nInctvAmt") * p_oRecord.getDouble("nAllcPerc") / 100) + p_oRecord.getDouble("nAllcAmtx");
                double dedctAmt = (p_oRecord.getDouble("nDedctAmt") * p_oRecord.getDouble("xDedAlcPer") / 100) + p_oRecord.getDouble("xDedAlcAmt");

                System.out.println(p_oRecord.getString("sTransNox") + " "
                        + p_oRecord.getString("sEmployID") + " "
                        + p_oRecord.getDouble("nTotalAmt") + " ");
                if (currentEmployID == null || !currentEmployID.equals(employID)) {
                    if (currentEmployID != null) {

                        // Insert the previous group into p_oRecordProcessed
                        p_oRecordProcessed.last();
                        p_oRecordProcessed.moveToInsertRow();
                        p_oRecord.absolute(p_oRecord.getRow() - 1);
                        copyCurrentRow(p_oRecord, p_oRecordProcessed);
//                        System.out.println(p_oRecord.getString("sTransNox") + "  Employname  " + p_oRecord.getString("sEmployID"));
                        p_oRecordProcessed.updateDouble("nInctvAmt", xInctvAmt);
                        p_oRecordProcessed.updateDouble("nDedctAmt", xDedctAmt);

                        xNetAmount = xInctvAmt - xDedctAmt;
                        p_oRecordProcessed.updateDouble("nTotalAmt", xNetAmount);
                        p_oRecordProcessed.insertRow();
                        p_oRecordProcessed.moveToCurrentRow();

                        // Start a new group
                        p_oRecord.absolute(p_oRecord.getRow() + 1);
                        xInctvAmt = 0;
                        xDedctAmt = 0;
                        xNetAmount = 0;

                        currentEmployID = employID;
                        xInctvAmt += inctvAmt;
                        xDedctAmt += dedctAmt;

                    } else {//handle first record
                        xInctvAmt += inctvAmt;
                        xDedctAmt += dedctAmt;
                        currentEmployID = employID;
                    }

                } else {
                    // Accumulate the sums
                    xInctvAmt += inctvAmt;
                    xDedctAmt += dedctAmt;
                }
            }

            // Ensure the last group is inserted
            if (currentEmployID != null) {
                p_oRecordProcessed.last();
                p_oRecordProcessed.moveToInsertRow();
                p_oRecord.last();

                copyCurrentRow(p_oRecord, p_oRecordProcessed);
                p_oRecordProcessed.updateDouble("nInctvAmt", xInctvAmt);
                p_oRecordProcessed.updateDouble("nDedctAmt", xDedctAmt);

                xNetAmount = xInctvAmt - xDedctAmt;
                p_oRecordProcessed.updateDouble("nTotalAmt", xNetAmount);
                p_oRecordProcessed.insertRow();
                p_oRecordProcessed.moveToCurrentRow();
            }

            return true;
        } catch (SQLException ex) {

            Logger.getLogger(IncentiveReportNew.class.getName()).log(Level.SEVERE, null, ex);
            p_sMessage = ex.getMessage();
            return false;
        }
    }

    public boolean procReportSummarizedBranchCategory() {
        try {
            if (p_oRecord == null) {
                return false;
            }
            // Variables to track the current group and sums
            String currentBranchCode = null;
            String currentIncCategory = null;
            p_oRecordProcessed.beforeFirst();
            p_oRecord.beforeFirst();
            double xInctvAmt = 0;
            double xDedctAmt = 0;
            double xNetAmount = 0;

            while (p_oRecord.next()) {
                String branchCd = p_oRecord.getString("sBranchCd");
                String inctveCd = p_oRecord.getString("sInctveCD");
                double inctvAmt = (p_oRecord.getDouble("nInctvAmt") * p_oRecord.getDouble("nAllcPerc") / 100) + p_oRecord.getDouble("nAllcAmtx");
                double dedctAmt = (p_oRecord.getDouble("nDedctAmt") * p_oRecord.getDouble("xDedAlcPer") / 100) + p_oRecord.getDouble("xDedAlcAmt");

//               
                if (currentBranchCode == null
                        || currentIncCategory == null
                        || !currentBranchCode.equals(branchCd)
                        || !currentIncCategory.equals(inctveCd)) {
                    if (currentIncCategory != null) {

                        // Insert the previous group into p_oRecordProcessed
                        p_oRecordProcessed.last();
                        p_oRecordProcessed.moveToInsertRow();
                        p_oRecord.absolute(p_oRecord.getRow() - 1);
                        copyCurrentRow(p_oRecord, p_oRecordProcessed);
                        p_oRecordProcessed.updateDouble("nInctvAmt", xInctvAmt);
                        p_oRecordProcessed.updateDouble("nDedctAmt", xDedctAmt);

                        xNetAmount = xInctvAmt - xDedctAmt;
                        p_oRecordProcessed.updateDouble("nTotalAmt", xNetAmount);
                        p_oRecordProcessed.insertRow();
                        p_oRecordProcessed.moveToCurrentRow();

                        // Start a new group
                        p_oRecord.absolute(p_oRecord.getRow() + 1);
                        xInctvAmt = 0;
                        xDedctAmt = 0;
                        xNetAmount = 0;

                        currentBranchCode = branchCd;
                        currentIncCategory = inctveCd;
                        xInctvAmt += inctvAmt;
                        xDedctAmt += dedctAmt;

                    } else {//handle first record
                        xInctvAmt += inctvAmt;
                        xDedctAmt += dedctAmt;
                        currentBranchCode = branchCd;
                        currentIncCategory = inctveCd;
                    }

                } else {
                    // Accumulate the sums
                    xInctvAmt += inctvAmt;
                    xDedctAmt += dedctAmt;
                }
            }

            // Ensure the last group is inserted
            if (currentIncCategory != null) {
                p_oRecordProcessed.last();
                p_oRecordProcessed.moveToInsertRow();
                p_oRecord.last();

                copyCurrentRow(p_oRecord, p_oRecordProcessed);
                p_oRecordProcessed.updateDouble("nInctvAmt", xInctvAmt);
                p_oRecordProcessed.updateDouble("nDedctAmt", xDedctAmt);
                xNetAmount = xInctvAmt - xDedctAmt;
                p_oRecordProcessed.updateDouble("nTotalAmt", xNetAmount);
                p_oRecordProcessed.insertRow();
                p_oRecordProcessed.moveToCurrentRow();
            }

            return true;
        } catch (SQLException ex) {

            Logger.getLogger(IncentiveReportNew.class.getName()).log(Level.SEVERE, null, ex);
            p_sMessage = ex.getMessage();
            return false;
        }
    }

    public boolean procReportSummarizedEmployeeCategory() {
        try {
            if (p_oRecord == null) {
                return false;
            }
            // Variables to track the current group and sums
            String currentBranchCode = null;
            String currentEmployID = null;
            String currentIncCategory = null;
            p_oRecordProcessed.beforeFirst();
            p_oRecord.beforeFirst();
            double xInctvAmt = 0;
            double xDedctAmt = 0;
            double xNetAmount = 0;

            while (p_oRecord.next()) {
                String branchCd = p_oRecord.getString("sBranchCd");
                String employID = p_oRecord.getString("sEmployID");
                String inctveCd = p_oRecord.getString("sInctveCD");
                double inctvAmt = (p_oRecord.getDouble("nInctvAmt") * p_oRecord.getDouble("nAllcPerc") / 100) + p_oRecord.getDouble("nAllcAmtx");
                double dedctAmt = (p_oRecord.getDouble("nDedctAmt") * p_oRecord.getDouble("xDedAlcPer") / 100) + p_oRecord.getDouble("xDedAlcAmt");

//               
                if (currentBranchCode == null
                        || currentBranchCode == null
                        || currentEmployID == null
                        || !currentBranchCode.equals(branchCd)
                        || !currentEmployID.equals(employID)
                        || !currentIncCategory.equals(inctveCd)) {
                    if (currentIncCategory != null) {

                        // Insert the previous group into p_oRecordProcessed
                        p_oRecordProcessed.last();
                        p_oRecordProcessed.moveToInsertRow();
                        p_oRecord.absolute(p_oRecord.getRow() - 1);
                        copyCurrentRow(p_oRecord, p_oRecordProcessed);
                        p_oRecordProcessed.updateDouble("nInctvAmt", xInctvAmt);
                        p_oRecordProcessed.updateDouble("nDedctAmt", xDedctAmt);

                        xNetAmount = xInctvAmt - xDedctAmt;
                        p_oRecordProcessed.updateDouble("nTotalAmt", xNetAmount);
                        p_oRecordProcessed.insertRow();
                        p_oRecordProcessed.moveToCurrentRow();

                        // Start a new group
                        p_oRecord.absolute(p_oRecord.getRow() + 1);
                        xInctvAmt = 0;
                        xDedctAmt = 0;
                        xNetAmount = 0;

                        currentBranchCode = branchCd;
                        currentEmployID = employID;
                        currentIncCategory = inctveCd;
                        xInctvAmt += inctvAmt;
                        xDedctAmt += dedctAmt;

                    } else {//handle first record
                        xInctvAmt += inctvAmt;
                        xDedctAmt += dedctAmt;
                        currentBranchCode = branchCd;
                        currentEmployID = employID;
                        currentIncCategory = inctveCd;
                    }

                } else {
                    // Accumulate the sums
                    xInctvAmt += inctvAmt;
                    xDedctAmt += dedctAmt;
                }
            }

            // Ensure the last group is inserted
            if (currentIncCategory != null) {
                p_oRecordProcessed.last();
                p_oRecordProcessed.moveToInsertRow();
                p_oRecord.last();

                copyCurrentRow(p_oRecord, p_oRecordProcessed);
                p_oRecordProcessed.updateDouble("nInctvAmt", xInctvAmt);
                p_oRecordProcessed.updateDouble("nDedctAmt", xDedctAmt);
                xNetAmount = xInctvAmt - xDedctAmt;
                p_oRecordProcessed.updateDouble("nTotalAmt", xNetAmount);
                p_oRecordProcessed.insertRow();
                p_oRecordProcessed.moveToCurrentRow();
                p_oRecord.absolute(p_oRecord.getRow() + 1);
            }

            return true;
        } catch (SQLException ex) {

            Logger.getLogger(IncentiveReportNew.class.getName()).log(Level.SEVERE, null, ex);
            p_sMessage = ex.getMessage();
            return false;
        }
    }

    private void copyCurrentRow(CachedRowSet source, CachedRowSet target) throws SQLException {
        ResultSetMetaData metaData = source.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            target.updateObject(i, source.getObject(i));
        }
    }

    public boolean procReportDetailedReport() {
        try {
            if (p_oRecord == null) {
                return false;
            }

            p_oRecordProcessed.beforeFirst();
            p_oRecord.beforeFirst();

            while (p_oRecord.next()) {
                processDetailedRecord(p_oRecord, p_oRecordProcessed);
            }

            return true;
        } catch (SQLException ex) {
            Logger.getLogger(IncentiveReportNew.class.getName()).log(Level.SEVERE, null, ex);
            p_sMessage = ex.getMessage();
            return false;
        }
    }

    private void processDetailedRecord(CachedRowSet source, CachedRowSet destination) throws SQLException {
        
        double xInctvAmt = (source.getDouble("nInctvAmt") * source.getDouble("nAllcPerc") / 100) + source.getDouble("nAllcAmtx");
        double xDedctAmt = (source.getDouble("nDedctAmt") * source.getDouble("xDedAlcPer") / 100) + source.getDouble("xDedAlcAmt");
        
        double xNetAmount = xInctvAmt - xDedctAmt;
//            System.out.println(source.getString("sTransNox") + source.getString("sCompnyNm") +"Total to Reprot ="+ xNetAmount);
        destination.last();
        destination.moveToInsertRow();
        copyCurrentRow(source, destination);
        destination.updateDouble("nInctvAmt", xInctvAmt);
        destination.updateDouble("nDedctAmt", xDedctAmt);
        destination.updateDouble("nTotalAmt", xNetAmount);
        destination.insertRow();
        destination.moveToCurrentRow();
    }

    public String getSQ_Branch() {
        String lsSQL = "";

        lsSQL = "SELECT"
                + "  sBranchCd "
                + " , sBranchNm xxColName "
                + " FROM Branch "
                + " WHERE cRecdStat = 1";

        return lsSQL;
    }

    public boolean OpenBranch(String fsBranch) throws SQLException {
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
        lsSQL = MiscUtil.addCondition(getSQ_Branch(), "sBranchCd = " + SQLUtil.toSQL(fsBranch));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oBranch = factory.createCachedRowSet();
        p_oBranch.populate(loRS);
        MiscUtil.close(loRS);

        p_nEditMode = EditMode.READY;

        return true;
    }

    public boolean searchBranch(String fsValue, boolean fbByCode) throws SQLException {

        String lsSQL = getSQ_Branch();
        if (fbByCode) {
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchCd = " + SQLUtil.toSQL(fsValue));
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }

        JSONObject loJSON;

        if (p_bWithUI) {
            loJSON = showFXDialog.jsonSearch(
                    p_oApp,
                    lsSQL,
                    fsValue,
                    "Code»Branch Name",
                    "sBranchCd»xxColName",
                    "sBranchCd»sBranchNm",
                    fbByCode ? 0 : 1);

            if (loJSON != null) {
                return OpenBranch((String) loJSON.get("sBranchCd"));
            } else {
                p_sMessage = "No record selected.";
                return false;
            }
        }

        if (fbByCode) {
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchCd = " + SQLUtil.toSQL(fsValue));
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
            lsSQL += " LIMIT 1";
        }

        ResultSet loRS = p_oApp.executeQuery(lsSQL);

        if (!loRS.next()) {
            MiscUtil.close(loRS);
            p_sMessage = "No branch found for the given criteria.";
            return false;
        }

        String lsCode = loRS.getString("sBranchCd");
        MiscUtil.close(loRS);

        return OpenBranch(lsCode);
    }

    public String getSQ_Division() {
        String lsSQL = "";

        lsSQL = "SELECT"
                + "  sDivsnCde "
                + " , sDivsnDsc xxColName"
                + " FROM Division "
                + " WHERE cRecdStat = 1";

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
                    "sDivsnCde»xxColName",
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

    public String getSQ_Category() {
        String lsSQL = "";

        lsSQL = "SELECT sInctveCD, xxColName "
                + " FROM "
                + "( SELECT sInctveCD "
                + " , sInctveDs xxColName"
                + " , cRecdStat "
                + " FROM Incentive "
                + " WHERE cRecdStat = '1' "
                + " UNION "
                + " SELECT "
                + " '999' sInctveCD "
                + " , 'Deduction' xxColName"
                + " , '1' cRecdStat ) Incentive_Category "
                + " WHERE cRecdStat = '1' ";

        return lsSQL;
    }

    public boolean OpenCategory(String fsCategory) throws SQLException {
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
        lsSQL = MiscUtil.addCondition(getSQ_Category(), "sInctveCD = " + SQLUtil.toSQL(fsCategory));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oCategory = factory.createCachedRowSet();
        p_oCategory.populate(loRS);
        MiscUtil.close(loRS);

        p_nEditMode = EditMode.READY;

        return true;
    }

    public boolean searchCategory(String fsValue, boolean fbByCode) throws SQLException {

        String lsSQL = getSQ_Category();
        if (fbByCode) {
            lsSQL = MiscUtil.addCondition(lsSQL, "sInctveCD = " + SQLUtil.toSQL(fsValue));
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "xxColName LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }

        JSONObject loJSON;

        if (p_bWithUI) {
            loJSON = showFXDialog.jsonSearch(
                    p_oApp,
                    lsSQL,
                    fsValue,
                    "Code»Incentive Type",
                    "sInctveCD»xxColName",
                    "sInctveCD»xxColName",
                    fbByCode ? 0 : 1);

            if (loJSON != null) {
                return OpenCategory((String) loJSON.get("sInctveCD"));
            } else {
                p_sMessage = "No record selected.";
                return false;
            }
        }

        if (fbByCode) {
            lsSQL = MiscUtil.addCondition(lsSQL, "sInctveCD = " + SQLUtil.toSQL(fsValue));
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "xxColName LIKE " + SQLUtil.toSQL(fsValue + "%"));
            lsSQL += " LIMIT 1";
        }

        ResultSet loRS = p_oApp.executeQuery(lsSQL);

        if (!loRS.next()) {
            MiscUtil.close(loRS);
            p_sMessage = "No Division found for the given criteria.";
            return false;
        }

        String lsCode = loRS.getString("sInctveCD");
        MiscUtil.close(loRS);

        return OpenCategory(lsCode);
    }

    public String getSQ_BranchArea() {
        String lsSQL = "";

        lsSQL = " SELECT sAreaCode "
                + " , sAreaDesc xxColName "
                + " FROM Branch_Area "
                + " WHERE cRecdStat = '1' ";

        return lsSQL;
    }

    public boolean OpenBranchArea(String fsArea) throws SQLException {
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
        lsSQL = MiscUtil.addCondition(getSQ_BranchArea(), "sAreaCode = " + SQLUtil.toSQL(fsArea));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oBranchArea = factory.createCachedRowSet();
        p_oBranchArea.populate(loRS);
        MiscUtil.close(loRS);

        p_nEditMode = EditMode.READY;

        return true;
    }

    public boolean searchBranchArea(String fsValue, boolean fbByCode) throws SQLException {

        String lsSQL = getSQ_BranchArea();
        if (fbByCode) {
            lsSQL = MiscUtil.addCondition(lsSQL, "sAreaCode = " + SQLUtil.toSQL(fsValue));
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "sAreaDesc LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }

        JSONObject loJSON;

        if (p_bWithUI) {
            loJSON = showFXDialog.jsonSearch(
                    p_oApp,
                    lsSQL,
                    fsValue,
                    "Code»Area Name",
                    "sAreaCode»xxColName",
                    "sAreaCode»sAreaDesc",
                    fbByCode ? 0 : 1);

            if (loJSON != null) {
                return OpenBranchArea((String) loJSON.get("sAreaCode"));
            } else {
                p_sMessage = "No record selected.";
                return false;
            }
        }

        if (fbByCode) {
            lsSQL = MiscUtil.addCondition(lsSQL, "sAreaCode = " + SQLUtil.toSQL(fsValue));
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "sAreaDesc LIKE " + SQLUtil.toSQL(fsValue + "%"));
            lsSQL += " LIMIT 1";
        }

        ResultSet loRS = p_oApp.executeQuery(lsSQL);

        if (!loRS.next()) {
            MiscUtil.close(loRS);
            p_sMessage = "No Division found for the given criteria.";
            return false;
        }

        String lsCode = loRS.getString("sAreaDesc");
        MiscUtil.close(loRS);

        return OpenBranchArea(lsCode);
    }

    public boolean searchFilter(int fiIndex, String fsValue, boolean fbByCode) throws SQLException {
        switch (fiIndex) {
            case 2:
                return searchBranch(fsValue, fbByCode);
            case 3:
                return searchDivision(fsValue, fbByCode);
            case 4:
                return searchCategory(fsValue, fbByCode);
            case 5:
                return searchBranchArea(fsValue, fbByCode);
            default:
                return false;
        }
    }
}
