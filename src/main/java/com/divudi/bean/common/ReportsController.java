package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.CashBookEntryController;
import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.DrawerEntryController;
import com.divudi.bean.channel.ChannelSearchController;
import com.divudi.bean.channel.analytics.ReportTemplateController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.opd.OpdBillController;
import com.divudi.bean.pharmacy.PharmacyBillSearch;
import com.divudi.bean.pharmacy.PharmacyPreSettleController;
import com.divudi.bean.pharmacy.PharmacySaleBhtController;
import com.divudi.data.*;
import com.divudi.data.analytics.ReportTemplateType;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.*;
import com.divudi.entity.cashTransaction.CashBookEntry;
import com.divudi.entity.cashTransaction.Drawer;
import com.divudi.entity.inward.Admission;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.inward.RoomCategory;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientReport;
import com.divudi.facade.*;
import com.divudi.java.CommonFunctions;
import com.divudi.light.common.BillLight;
import com.divudi.light.common.BillSummaryRow;
import com.divudi.service.BillService;
import com.divudi.service.PatientInvestigationService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;
import java.text.DecimalFormat;

import org.apache.poi.xssf.usermodel.XSSFCellStyle;

/**
 * @author safrin
 */
@Named
@SessionScoped
public class ReportsController implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * EJBs
     */
    private CommonFunctions commonFunctions;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    BillSessionFacade billSessionFacade;
    @EJB
    StockFacade stockFacade;
    @EJB
    PatientReportFacade patientReportFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    TokenFacade tokenFacade;
    @EJB
    private DrawerFacade drawerFacade;
    @EJB
    BillService billService;
    @EJB
    PatientInvestigationService patientInvestigationService;
    @EJB
    PatientEncounterFacade peFacade;
    List<PatientEncounter> patientEncounters;

    /**
     * Inject
     */
    @Inject
    private BillBeanController billBean;
    @Inject
    private SessionController sessionController;
    @Inject
    TransferController transferController;
    @Inject
    private CommonController commonController;
    @Inject
    PharmacySaleBhtController pharmacySaleBhtController;
    @Inject
    SmsController smsController;
    @Inject
    AuditEventApplicationController auditEventApplicationController;
    @Inject
    WebUserController webUserController;
    @Inject
    OpdPreSettleController opdPreSettleController;
    @Inject
    PharmacyPreSettleController pharmacyPreSettleController;
    @Inject
    TokenController tokenController;
    @Inject
    private DepartmentController departmentController;
    @Inject
    BillSearch billSearch;
    @Inject
    PharmacyBillSearch pharmacyBillSearch;
    @Inject
    OpdBillController opdBillController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    ChannelSearchController channelSearchController;
    @Inject
    ReportTemplateController reportTemplateController;
    @Inject
    CashBookEntryController cashBookEntryController;
    @Inject
    ExcelController excelController;
    @Inject
    PdfController pdfController;
    @Inject
    DrawerEntryController drawerEntryController;
    @Inject
    DrawerController drawerController;
    @Inject
    EnumController enumController;

    /**
     * Properties
     */
    private String visitType;
    private String methodType;

    private Category category;
    private ReportTemplateType reportTemplateType;
    private SearchKeyword searchKeyword;
    private Date fromDate;
    private Date toDate;
    private Long startBillId;
    private Long endBillId;
    private WebUser webUser;
    private String backLink;
    private int maxResult = 50;
    private BillType billType;
    private PaymentMethod paymentMethod;
    private List<PaymentMethod> paymentMethods;
    private List<Bill> bills;
    private List<Payment> payments;
    private List<BillLight> billLights;
    private List<BillSummaryRow> billSummaryRows;
    private List<Bill> selectedBills;
    private List<Bill> aceptPaymentBills;
    private List<BillFee> billFees;
    private List<BillFee> billFeesDone;
    private List<BillItem> billItems;
    private List<PatientInvestigation> patientInvestigations;
    private List<PatientReport> patientReports;
    private List<PatientInvestigation> patientInvestigationsSigle;
    private BillTypeAtomic billTypeAtomic;
    private BillClassType billClassType;
    private Institution collectingCentre;
    private RoomCategory roomCategory;

    private StreamedContent downloadingExcel;

    BillSummaryRow billSummaryRow;
    Bill cancellingIssueBill;
    Bill bill;
    Speciality speciality;
    PatientEncounter patientEncounter;
    Staff staff;
    Item item;
    double dueTotal;
    double doneTotal;
    double netTotal;
    private double totalBillCount;
    private double grossTotal;
    private double discount;
    ServiceSession selectedServiceSession;
    Staff currentStaff;
    private String mrnNo;
    List<BillItem> billItem;
    List<PatientInvestigation> userPatientInvestigations;
    double netTotalValue;

    private ItemLight investigationCode;

    String menuBarSearchText;
    String smsText;
    String uniqueSmsText;
    boolean channelingPanelVisible;
    boolean pharmacyPanelVisible;
    boolean opdPanelVisible;
    boolean inwardPanelVisible;
    boolean labPanelVisile;
    boolean patientPanelVisible;
    ReportKeyWord reportKeyWord;
    private Route route;

    List<Bill> channellingBills;
    List<BillSession> billSessions;
    List<Bill> opdBills;
    List<Bill> pharmacyBills;
    List<Admission> admissions;
    List<PatientInvestigation> pis;
    List<Patient> patients;
    List<String> telephoneNumbers;
    List<String> selectedTelephoneNumbers;

    BillSession selectedBillSession;
    UploadedFile file;
    private Institution creditCompany;

    private Institution otherInstitution;

    private Institution institution;
    private Department department;
    List<Bill> prescreptionBills;
    private Department fromDepartment;
    private Department toDepartment;
    private Institution fromInstitution;
    private Institution toInstitution;
    private int manageListIndex;
    private Patient patient;
    private Institution dealer;
    private List<Bill> grnBills;
    Bill currentBill;
    private Long currentBillId;
    private Bill preBill;
    boolean billPreview;
    private Long barcodeIdLong;
    private Date maxDate;
    private Doctor referingDoctor;
    private Month month;
    private PaymentScheme paymentScheme;
    private String remarks;

    private double cashTotal;
    private double cardTotal;
    private double chequeTotal;
    private double slipTotal;
    private double totalOfOtherPayments;
    private double billCount;
    private Token token;
    private int managePaymentIndex = -1;

    private double hosTotal;
    private double staffTotal;
    private double discountTotal;
    private double amountTotal;
    double totalPaying;

    private String cashBookNumber;
    private String invoiceNumber;

    private boolean duplicateBillView;

    private ReportTemplateRowBundle bundle;
    private ReportTemplateRowBundle bundleBillItems;

    private List<CashBookEntry> cashBookEntries;
    private Institution site;
    private Institution toSite;
    private List<Drawer> drawerList;
    private Drawer selectedDrawer;
    private int opdAnalyticsIndex;
    private AdmissionType admissionType;
    private List<AgentHistory> agentHistories;

    private String searchType;
    private String reportType;
    private String serviceGroup;
    private boolean withProfessionalFee;

    private Drawer drawer;

    private Department serviceDepartment;
    private Department billedDepartment;
    private List<Department> departments;

    private Map<Integer, Map<String, Map<Integer, Double>>> groupedBillItemsWeekly;
    private Map<String, Map<Integer, Double>> groupedBillItemsWeeklyValues7to7;
    private Map<String, Map<Integer, Double>> groupedBillItemsWeeklyValues7to1;
    private Map<String, Map<Integer, Double>> groupedBillItemsWeeklyValues1to7;

    double total;
    double paid;
    double creditPaid;
    double creditUsed;
    double calTotal;
    double totalVat;
    double totalVatCalculatedValue;

    private Map<Institution, Map<YearMonth, Bill>> groupedCollectingCenterWiseBillsMonthly;
    private Map<Route, Map<YearMonth, Bill>> groupedRouteWiseBillsMonthly;
    private List<YearMonth> yearMonths;

    private String settlementStatus;
    private String dischargedStatus;

    private String selectedDateType = "invoice";

    private Investigation investigation;

    // Map<Week, Map<ItemName, Map<dayOfMonth, Count>>>
    Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap7to7;
    Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap7to1;
    Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap1to7;

    private boolean showChart;

    public String getDischargedStatus() {
        return dischargedStatus;
    }

    public void setDischargedStatus(String dischargedStatus) {
        this.dischargedStatus = dischargedStatus;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public PatientReportFacade getPatientReportFacade() {
        return patientReportFacade;
    }

    public void setPatientReportFacade(PatientReportFacade patientReportFacade) {
        this.patientReportFacade = patientReportFacade;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getSelectedDateType() {
        return selectedDateType;
    }

    public void setSelectedDateType(String selectedDateType) {
        this.selectedDateType = selectedDateType;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public String getServiceGroup() {
        return serviceGroup;
    }

    public void setServiceGroup(String serviceGroup) {
        this.serviceGroup = serviceGroup;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public Map<Integer, Map<String, Map<Integer, Double>>> getWeeklyDailyBillItemMap7to7() {
        return weeklyDailyBillItemMap7to7;
    }

    public void setWeeklyDailyBillItemMap7to7(Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap7to7) {
        this.weeklyDailyBillItemMap7to7 = weeklyDailyBillItemMap7to7;
    }

    public Map<Integer, Map<String, Map<Integer, Double>>> getWeeklyDailyBillItemMap7to1() {
        return weeklyDailyBillItemMap7to1;
    }

    public void setWeeklyDailyBillItemMap7to1(Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap7to1) {
        this.weeklyDailyBillItemMap7to1 = weeklyDailyBillItemMap7to1;
    }

    public Map<Integer, Map<String, Map<Integer, Double>>> getWeeklyDailyBillItemMap1to7() {
        return weeklyDailyBillItemMap1to7;
    }

    public void setWeeklyDailyBillItemMap1to7(Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap1to7) {
        this.weeklyDailyBillItemMap1to7 = weeklyDailyBillItemMap1to7;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public List<PatientReport> getPatientReports() {
        return patientReports;
    }

    public void setPatientReports(List<PatientReport> patientReports) {
        this.patientReports = patientReports;
    }

    public Institution getOtherInstitution() {
        return otherInstitution;
    }

    public void setOtherInstitution(Institution otherInstitution) {
        this.otherInstitution = otherInstitution;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public int getManageListIndex() {
        return manageListIndex;
    }

    public void setManageListIndex(int manageListIndex) {
        this.manageListIndex = manageListIndex;
    }

    public List<BillLight> getBillLights() {
        return billLights;
    }

    public void setBillLights(List<BillLight> billLights) {
        this.billLights = billLights;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Doctor getReferingDoctor() {
        return referingDoctor;
    }

    public void setReferingDoctor(Doctor referingDoctor) {
        this.referingDoctor = referingDoctor;
    }

    public Institution getDealer() {
        return dealer;
    }

    public void setDealer(Institution dealer) {
        this.dealer = dealer;
    }

    public List<Bill> getGrnBills() {
        return grnBills;
    }

    public void setGrnBills(List<Bill> grnBills) {
        this.grnBills = grnBills;
    }

    public Long getCurrentBillId() {
        return currentBillId;
    }

    public void setCurrentBillId(Long currentBillId) {
        this.currentBillId = currentBillId;
    }

    public Long getBarcodeIdLong() {
        return barcodeIdLong;
    }

    public void setBarcodeIdLong(Long barcodeIdLong) {
        this.barcodeIdLong = barcodeIdLong;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public List<BillSummaryRow> getBillSummaryRows() {
        return billSummaryRows;
    }

    public void setBillSummaryRows(List<BillSummaryRow> billSummaryRows) {
        this.billSummaryRows = billSummaryRows;
    }

    public double getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public DepartmentController getDepartmentController() {
        return departmentController;
    }

    public void setDepartmentController(DepartmentController departmentController) {
        this.departmentController = departmentController;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public double getTotalBillCount() {
        return totalBillCount;
    }

    public void setTotalBillCount(double totalBillCount) {
        this.totalBillCount = totalBillCount;
    }

    public Long getStartBillId() {
        return startBillId;
    }

    public void setStartBillId(Long startBillId) {
        this.startBillId = startBillId;
    }

    public Long getEndBillId() {
        return endBillId;
    }

    public void setEndBillId(Long endBillId) {
        this.endBillId = endBillId;
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public Date getMaxDate() {
        maxDate = commonFunctions.getEndOfDay(new Date());
        return maxDate;
    }

    public Map<Integer, Map<String, Map<Integer, Double>>> getGroupedBillItemsWeekly() {
        return groupedBillItemsWeekly;
    }

    public void setGroupedBillItemsWeekly(Map<Integer, Map<String, Map<Integer, Double>>> groupedBillItemsWeekly) {
        this.groupedBillItemsWeekly = groupedBillItemsWeekly;
    }

    public Map<String, Map<Integer, Double>> getGroupedBillItemsWeeklyValues7to7() {
        return groupedBillItemsWeeklyValues7to7;
    }

    public void setGroupedBillItemsWeeklyValues7to7(Map<String, Map<Integer, Double>> groupedBillItemsWeeklyValues7to7) {
        this.groupedBillItemsWeeklyValues7to7 = groupedBillItemsWeeklyValues7to7;
    }

    public Map<String, Map<Integer, Double>> getGroupedBillItemsWeeklyValues7to1() {
        return groupedBillItemsWeeklyValues7to1;
    }

    public void setGroupedBillItemsWeeklyValues7to1(Map<String, Map<Integer, Double>> groupedBillItemsWeeklyValues7to1) {
        this.groupedBillItemsWeeklyValues7to1 = groupedBillItemsWeeklyValues7to1;
    }

    public Map<String, Map<Integer, Double>> getGroupedBillItemsWeeklyValues1to7() {
        return groupedBillItemsWeeklyValues1to7;
    }

    public void setGroupedBillItemsWeeklyValues1to7(Map<String, Map<Integer, Double>> groupedBillItemsWeeklyValues1to7) {
        this.groupedBillItemsWeeklyValues1to7 = groupedBillItemsWeeklyValues1to7;
    }

    public void setMaxDate(Date maxDate) {
        this.maxDate = maxDate;
    }

    public double getCashTotal() {
        return cashTotal;
    }

    public void setCashTotal(double cashTotal) {
        this.cashTotal = cashTotal;
    }

    public double getCardTotal() {
        return cardTotal;
    }

    public void setCardTotal(double cardTotal) {
        this.cardTotal = cardTotal;
    }

    public double getChequeTotal() {
        return chequeTotal;
    }

    public void setChequeTotal(double chequeTotal) {
        this.chequeTotal = chequeTotal;
    }

    public double getSlipTotal() {
        return slipTotal;
    }

    public void setSlipTotal(double slipTotal) {
        this.slipTotal = slipTotal;
    }

    public double getTotalOfOtherPayments() {
        return totalOfOtherPayments;
    }

    public void setTotalOfOtherPayments(double totalOfOtherPayments) {
        this.totalOfOtherPayments = totalOfOtherPayments;
    }

    public double getBillCount() {
        return billCount;
    }

    public void setBillCount(double billCount) {
        this.billCount = billCount;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public String getCashBookNumber() {
        return cashBookNumber;
    }

    public void setCashBookNumber(String cashBookNumber) {
        this.cashBookNumber = cashBookNumber;
    }

    public boolean isDuplicateBillView() {
        return duplicateBillView;
    }

    public void setDuplicateBillView(boolean duplicateBillView) {
        this.duplicateBillView = duplicateBillView;
    }

    public int getManagePaymentIndex() {
        return managePaymentIndex;
    }

    public void setManagePaymentIndex(int managePaymentIndex) {
        this.managePaymentIndex = managePaymentIndex;
    }

    public ReportTemplateType getReportTemplateType() {
        return reportTemplateType;
    }

    public void setReportTemplateType(ReportTemplateType reportTemplateType) {
        this.reportTemplateType = reportTemplateType;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<CashBookEntry> getCashBookEntries() {
        return cashBookEntries;
    }

    public void setCashBookEntries(List<CashBookEntry> cashBookEntries) {
        this.cashBookEntries = cashBookEntries;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public ReportTemplateRowBundle getBundle() {
        return bundle;
    }

    public void setBundle(ReportTemplateRowBundle bundle) {
        this.bundle = bundle;
    }

    public String getBackLink() {
        return backLink;
    }

    public void setBackLink(String backLink) {
        this.backLink = backLink;
    }

    public ReportTemplateRowBundle getBundleBillItems() {
        return bundleBillItems;
    }

    public void setBundleBillItems(ReportTemplateRowBundle bundleBillItems) {
        this.bundleBillItems = bundleBillItems;
    }

    public int getOpdAnalyticsIndex() {
        return opdAnalyticsIndex;
    }

    public void setOpdAnalyticsIndex(int opdAnalyticsIndex) {
        this.opdAnalyticsIndex = opdAnalyticsIndex;
    }

    public List<Drawer> getDrawerList() {
        return drawerList;
    }

    public void setDrawerList(List<Drawer> drawerList) {
        this.drawerList = drawerList;
    }

    public DrawerFacade getDrawerFacade() {
        return drawerFacade;
    }

    public void setDrawerFacade(DrawerFacade drawerFacade) {
        this.drawerFacade = drawerFacade;
    }

    public Drawer getSelectedDrawer() {
        return selectedDrawer;
    }

    public void setSelectedDrawer(Drawer selectedDrawer) {
        this.selectedDrawer = selectedDrawer;
    }

    public BillClassType getBillClassType() {
        return billClassType;
    }

    public void setBillClassType(BillClassType billClassType) {
        this.billClassType = billClassType;
    }

    public StreamedContent getDownloadingExcel() {
        return downloadingExcel;
    }

    public void setDownloadingExcel(StreamedContent downloadingExcel) {
        this.downloadingExcel = downloadingExcel;
    }

    public double getHosTotal() {
        return hosTotal;
    }

    public void setHosTotal(double hosTotal) {
        this.hosTotal = hosTotal;
    }

    public double getStaffTotal() {
        return staffTotal;
    }

    public void setStaffTotal(double staffTotal) {
        this.staffTotal = staffTotal;
    }

    public double getDiscountTotal() {
        return discountTotal;
    }

    public void setDiscountTotal(double discountTotal) {
        this.discountTotal = discountTotal;
    }

    public double getAmountTotal() {
        return amountTotal;
    }

    public void setAmountTotal(double amountTotal) {
        this.amountTotal = amountTotal;
    }

    public Institution getToSite() {
        return toSite;
    }

    public void setToSite(Institution toSite) {
        this.toSite = toSite;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getMrnNo() {
        return mrnNo;
    }

    public void setMrnNo(String mrnNo) {
        this.mrnNo = mrnNo;
    }

    public boolean isWithProfessionalFee() {
        return withProfessionalFee;
    }

    public void setWithProfessionalFee(boolean withProfessionalFee) {
        this.withProfessionalFee = withProfessionalFee;
    }

    public String getVisitType() {
        return visitType;
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public Month getMonth() {
        return month;
    }

    public void setMonth(Month month) {
        this.month = month;
    }

    public String getMethodType() {
        return methodType;
    }

    public void setMethodType(String methodType) {
        this.methodType = methodType;
    }

    public Drawer getDrawer() {
        return drawer;
    }

    public void setDrawer(Drawer drawer) {
        this.drawer = drawer;
    }

    public Department getServiceDepartment() {
        return serviceDepartment;
    }

    public void setServiceDepartment(Department serviceDepartment) {
        this.serviceDepartment = serviceDepartment;
    }

    public Department getBilledDepartment() {
        return billedDepartment;
    }

    public void setBilledDepartment(Department billedDepartment) {
        this.billedDepartment = billedDepartment;
    }

    public List<Month> getMonths() {
        return Arrays.asList(Month.values());
    }

    public ReportsController() {
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public List<BillFee> getBillFees() {
        return billFees;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public List<PatientInvestigation> getPatientInvestigations() {
        return patientInvestigations;
    }

    public void setPatientInvestigations(List<PatientInvestigation> patientInvestigations) {
        this.patientInvestigations = patientInvestigations;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public int getMaxResult() {

        return maxResult;
    }

    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
    }

    public List<Bill> getSelectedBills() {
        if (selectedBills == null) {
            selectedBills = new ArrayList<>();
        }
        return selectedBills;
    }

    public void setSelectedBills(List<Bill> selectedBills) {
        this.selectedBills = selectedBills;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public TransferController getTransferController() {
        return transferController;
    }

    public void setTransferController(TransferController transferController) {
        this.transferController = transferController;
    }

    public void setPatientInvestigationsSigle(List<PatientInvestigation> patientInvestigationsSigle) {
        this.patientInvestigationsSigle = patientInvestigationsSigle;
    }

    public Bill getCancellingIssueBill() {
        return cancellingIssueBill;
    }

    public void setCancellingIssueBill(Bill cancellingIssueBill) {
        this.cancellingIssueBill = cancellingIssueBill;
    }

    public double getNetTotalValue() {
        return netTotalValue;
    }

    public void setNetTotalValue(double netTotalValue) {
        this.netTotalValue = netTotalValue;
    }

    public List<BillFee> getBillFeesDone() {
        return billFeesDone;
    }

    public void setBillFeesDone(List<BillFee> billFeesDone) {
        this.billFeesDone = billFeesDone;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public double getDueTotal() {
        return dueTotal;
    }

    public void setDueTotal(double dueTotal) {
        this.dueTotal = dueTotal;
    }

    public double getDoneTotal() {
        return doneTotal;
    }

    public void setDoneTotal(double doneTotal) {
        this.doneTotal = doneTotal;
    }

    public double getTotalPaying() {
        return totalPaying;
    }

    public void setTotalPaying(double totalPaying) {
        this.totalPaying = totalPaying;
    }

    public List<AgentHistory> getAgentHistories() {
        return agentHistories;
    }

    public void setAgentHistories(List<AgentHistory> agentHistories) {
        this.agentHistories = agentHistories;
    }

    public List<PatientInvestigation> getUserPatientInvestigations() {
        return userPatientInvestigations;
    }

    public void setUserPatientInvestigations(List<PatientInvestigation> userPatientInvestigations) {
        this.userPatientInvestigations = userPatientInvestigations;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public List<Bill> getAceptPaymentBills() {
        if (aceptPaymentBills == null) {
            aceptPaymentBills = new ArrayList<>();
        }
        return aceptPaymentBills;
    }

    public void setAceptPaymentBills(List<Bill> aceptPaymentBills) {
        this.aceptPaymentBills = aceptPaymentBills;
    }

    public String getMenuBarSearchText() {
        return menuBarSearchText;
    }

    public void setMenuBarSearchText(String menuBarSearchText) {
        this.menuBarSearchText = menuBarSearchText;
    }

    public boolean isChannelingPanelVisible() {
        return channelingPanelVisible;
    }

    public void setChannelingPanelVisible(boolean channelingPanelVisible) {
        this.channelingPanelVisible = channelingPanelVisible;
    }

    public boolean isPharmacyPanelVisible() {
        return pharmacyPanelVisible;
    }

    public void setPharmacyPanelVisible(boolean pharmacyPanelVisible) {
        this.pharmacyPanelVisible = pharmacyPanelVisible;
    }

    public boolean isOpdPanelVisible() {
        return opdPanelVisible;
    }

    public void setOpdPanelVisible(boolean opdPanelVisible) {
        this.opdPanelVisible = opdPanelVisible;
    }

    public boolean isInwardPanelVisible() {
        return inwardPanelVisible;
    }

    public void setInwardPanelVisible(boolean inwardPanelVisible) {
        this.inwardPanelVisible = inwardPanelVisible;
    }

    public boolean isLabPanelVisile() {
        return labPanelVisile;
    }

    public void setLabPanelVisile(boolean labPanelVisile) {
        this.labPanelVisile = labPanelVisile;
    }

    public boolean isPatientPanelVisible() {
        return patientPanelVisible;
    }

    public void setPatientPanelVisible(boolean patientPanelVisible) {
        this.patientPanelVisible = patientPanelVisible;
    }

    public List<Bill> getChannellingBills() {
        return channellingBills;
    }

    public void setChannellingBills(List<Bill> channellingBills) {
        this.channellingBills = channellingBills;
    }

    public List<Bill> getOpdBills() {
        return opdBills;
    }

    public void setOpdBills(List<Bill> opdBills) {
        this.opdBills = opdBills;
    }

    public List<Bill> getPharmacyBills() {
        return pharmacyBills;
    }

    public void setPharmacyBills(List<Bill> pharmacyBills) {
        this.pharmacyBills = pharmacyBills;
    }

    public List<Admission> getAdmissions() {
        return admissions;
    }

    public void setAdmissions(List<Admission> admissions) {
        this.admissions = admissions;
    }

    public List<PatientInvestigation> getPis() {
        return pis;
    }

    public void setPis(List<PatientInvestigation> pis) {
        this.pis = pis;
    }

    public List<Patient> getPatients() {
        return patients;
    }

    public void setPatients(List<Patient> patients) {
        this.patients = patients;
    }

    public List<BillSession> getBillSessions() {
        return billSessions;
    }

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

    public BillSession getSelectedBillSession() {
        return selectedBillSession;
    }

    public void setSelectedBillSession(BillSession selectedBillSession) {
        this.selectedBillSession = selectedBillSession;
    }

    public Date getCurrentDate() {
        return new Date();
    }

    public List<String> getTelephoneNumbers() {
        return telephoneNumbers;
    }

    public void setTelephoneNumbers(List<String> telephoneNumbers) {
        this.telephoneNumbers = telephoneNumbers;
    }

    public List<String> getSelectedTelephoneNumbers() {
        return selectedTelephoneNumbers;
    }

    public void setSelectedTelephoneNumbers(List<String> selectedTelephoneNumbers) {
        this.selectedTelephoneNumbers = selectedTelephoneNumbers;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    boolean paginator = true;
    int rows = 20;

    public boolean isPaginator() {
        return paginator;
    }

    public void setPaginator(boolean paginator) {
        this.paginator = paginator;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public String getSmsText() {
        return smsText;
    }

    public void setSmsText(String smsText) {
        this.smsText = smsText;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public String getUniqueSmsText() {
        return uniqueSmsText;
    }

    public void setUniqueSmsText(String uniqueSmsText) {
        this.uniqueSmsText = uniqueSmsText;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public PatientEncounterFacade getPeFacade() {
        return peFacade;
    }

    public void setPeFacade(PatientEncounterFacade peFacade) {
        this.peFacade = peFacade;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<PatientEncounter> getPatientEncounters() {
        return patientEncounters;
    }

    public void setPatientEncounters(List<PatientEncounter> patientEncounters) {
        this.patientEncounters = patientEncounters;
    }

    public ItemLight getInvestigationCode() {
        return investigationCode;
    }

    public void setInvestigationCode(ItemLight investigationCode) {
        this.investigationCode = investigationCode;
    }

    public double getPaid() {
        return paid;
    }

    public void setPaid(double paid) {
        this.paid = paid;
    }

    public RoomCategory getRoomCategory() {
        return roomCategory;
    }

    public void setRoomCategory(RoomCategory roomCategory) {
        this.roomCategory = roomCategory;
    }

    public double getCreditPaid() {
        return creditPaid;
    }

    public void setCreditPaid(double creditPaid) {
        this.creditPaid = creditPaid;
    }

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(String settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public Map<Institution, Map<YearMonth, Bill>> getGroupedCollectingCenterWiseBillsMonthly() {
        return groupedCollectingCenterWiseBillsMonthly;
    }

    public void setGroupedCollectingCenterWiseBillsMonthly(Map<Institution, Map<YearMonth, Bill>> groupedCollectingCenterWiseBillsMonthly) {
        this.groupedCollectingCenterWiseBillsMonthly = groupedCollectingCenterWiseBillsMonthly;
    }

    public List<YearMonth> getYearMonths() {
        return yearMonths;
    }

    public void setYearMonths(List<YearMonth> yearMonths) {
        this.yearMonths = yearMonths;
    }

    public Map<Route, Map<YearMonth, Bill>> getGroupedRouteWiseBillsMonthly() {
        return groupedRouteWiseBillsMonthly;
    }

    public void setGroupedRouteWiseBillsMonthly(Map<Route, Map<YearMonth, Bill>> groupedRouteWiseBillsMonthly) {
        this.groupedRouteWiseBillsMonthly = groupedRouteWiseBillsMonthly;
    }

    public boolean isShowChart() {
        return showChart;
    }

    public void setShowChart(boolean showChart) {
        this.showChart = showChart;
    }

    public void generateSampleCarrierReport() {
        System.out.println("generateSampleCarrierReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();

        if (visitType != null && visitType.equalsIgnoreCase("IP")) {
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
        } else {
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.OPD_BILL_REFUND);
        }

        System.out.println("bill items");

        bundle.setName("Sample Carrier Bill Items");
        bundle.setBundleType("billItemList");

        bundle = generateSampleCarrierBillItems(opdBts);
    }

    private ReportTemplateRowBundle generateSampleCarrierBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(pi) "
                + "FROM PatientInvestigation pi "
                + "JOIN pi.billItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE pi.retired=false "
                + " and billItem.retired=false "
                + " and bill.retired=false "
                + "AND pi.sampleSentAt IS NOT NULL ";

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (visitType != null) {
            if (visitType.equalsIgnoreCase("IP") || visitType.equalsIgnoreCase("OP")) {
                jpql += "AND bill.ipOpOrCc = :type ";
                parameters.put("type", visitType);
            }
        }

        if (staff != null) {
            jpql += "AND pi.sampleTransportedToLabByStaff.person.name = :staff ";
            parameters.put("staff", staff.getPerson().getName());
        }

        if (item != null) {
            jpql += "AND billItem.patientInvestigation.investigation.name = :item ";
            parameters.put("item", item.getName());
        }

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY pi";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        for (ReportTemplateRow row : rs) {
            BillItem billItem = row.getBillItem();
            PatientInvestigation investigation = row.getPatientInvestigation();

            if (investigation != null && investigation.getSampleSentAt() != null && investigation.getReceivedAt() != null) {
                long duration = investigation.getReceivedAt().getTime() - investigation.getSampleSentAt().getTime();
                row.setDuration(duration / (1000 * 60)); // in minutes
            } else {
                row.setDuration(0);
            }
        }

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generatePackageReport() {
        System.out.println("generatePackageReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();

        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);

        System.out.println("bill items");

        bundle.setName("Package Bill Items");
        bundle.setBundleType("billItemList");

        bundle = generateBillItems(opdBts);
    }

    private ReportTemplateRowBundle generateBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (visitType != null) {
            if (visitType.equalsIgnoreCase("IP") || visitType.equalsIgnoreCase("OP")) {
                jpql += "AND bill.ipOpOrCc = :type ";
                parameters.put("type", visitType);
            }
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().isEmpty()) {
            jpql += "AND ((bill.billPackege.name) like :itemName ) ";
            parameters.put("itemName", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().isEmpty()) {
            jpql += "AND ((bill.deptId) like :billNo ) ";
            parameters.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (item != null) {
            jpql += "AND billItem.item = :item ";
            parameters.put("item", item);
        }

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateOPDWeeklyReport() {
        System.out.println("generateOPDWeeklyReport = " + this);

        if (month == null) {
            JsfUtil.addErrorMessage("Please select a month");
            return;
        }

        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();

        if (visitType == null || visitType.equalsIgnoreCase("OP")) {
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.OPD_BILL_REFUND);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        }

        if (visitType == null || visitType.equalsIgnoreCase("IP")) {
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
            opdBts.add(BillTypeAtomic.CANCELLED_INWARD_FINAL_BILL);
            opdBts.add(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE_RETURN);
        }

//        if (visitType == null) {
//            opdBts.add(BillTypeAtomic.CC_BILL);
//            opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
//            opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);
//        }

        System.out.println("bill items");

        bundle.setName("Bill Items");
        bundle.setBundleType("billItemList");

        bundle = generateWeeklyBillItems(opdBts);

        if (reportType.equalsIgnoreCase("summary")) {
            groupBillItemsWeekly();
        } else if (reportType.equalsIgnoreCase("detail")) {
            groupBillItemsDaily();
        }
    }

    private void groupBillItemsDaily() {
        // Map<Week, Map<ItemName, Map<dayOfMonth, Count>>>
        Map<Integer, Map<String, Map<Integer, Double>>> weeklyBillItemMap7to7 = new HashMap<>();
        Map<Integer, Map<String, Map<Integer, Double>>> weeklyBillItemMap7to1 = new HashMap<>();
        Map<Integer, Map<String, Map<Integer, Double>>> weeklyBillItemMap1to7 = new HashMap<>();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            final BillItem billItem = row.getBillItem();

            final Date billItemDate = billItem.getBill().getCreatedAt();

            if (billItemDate == null || billItem.getItem() == null) {
                continue;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(billItemDate);

            final int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
            final int weekOfMonth = getWeekOfMonth(billItemDate);
            final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

            if (hourOfDay >= 19 || hourOfDay < 7) {
                // Between 7 PM to 7 AM
                Map<String, Map<Integer, Double>> billItemMap = weeklyBillItemMap7to7.containsKey(weekOfMonth) ? weeklyBillItemMap7to7.get(weekOfMonth) : new HashMap<>();

                billItemMap.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(dayOfMonth, billItemMap.get(billItem.getItem().getName()) != null
                                ? billItemMap.get(billItem.getItem().getName()).getOrDefault(dayOfMonth, 0.0) + billItem.getQty() : billItem.getQty());

                weeklyBillItemMap7to7.put(weekOfMonth, billItemMap);
            } else if (hourOfDay < 13) {
                // Between 7 AM to 1 PM
                Map<String, Map<Integer, Double>> billItemMap = weeklyBillItemMap7to1.containsKey(weekOfMonth) ? weeklyBillItemMap7to1.get(weekOfMonth) : new HashMap<>();

                billItemMap.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(dayOfMonth, billItemMap.get(billItem.getItem().getName()) != null
                                ? billItemMap.get(billItem.getItem().getName()).getOrDefault(dayOfMonth, 0.0) + billItem.getQty() : billItem.getQty());

                weeklyBillItemMap7to1.put(weekOfMonth, billItemMap);
            } else {
                // Between 1 PM to 7 PM
                Map<String, Map<Integer, Double>> billItemMap = weeklyBillItemMap1to7.containsKey(weekOfMonth) ? weeklyBillItemMap1to7.get(weekOfMonth) : new HashMap<>();

                billItemMap.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(dayOfMonth, billItemMap.get(billItem.getItem().getName()) != null
                                ? billItemMap.get(billItem.getItem().getName()).getOrDefault(dayOfMonth, 0.0) + billItem.getQty() : billItem.getQty());

                weeklyBillItemMap1to7.put(weekOfMonth, billItemMap);
            }
        }

        setWeeklyDailyBillItemMap7to7(weeklyBillItemMap7to7);
        setWeeklyDailyBillItemMap7to1(weeklyBillItemMap7to1);
        setWeeklyDailyBillItemMap1to7(weeklyBillItemMap1to7);
    }

    public List<String> getItemListByWeek(final int week, final Map<Integer, Map<String, Map<Integer, Double>>> weeklyBillItemMap) {
        if (weeklyBillItemMap == null) {
            return new ArrayList<>();
        }

        List<String> itemList = new ArrayList<>();

        if (weeklyBillItemMap.containsKey(week)) {
            itemList.addAll(weeklyBillItemMap.get(week).keySet());
        }

        return itemList;
    }

    public double getCountByWeekAndDay(final int week, final int day, final String itemName, final Map<Integer, Map<String, Map<Integer, Double>>> weeklyBillItemMap) {
        return Optional.ofNullable(weeklyBillItemMap)
                .map(map -> map.get(week))
                .map(weekMap -> weekMap.get(itemName))
                .map(itemMap -> itemMap.get(day))
                .orElse(0.0);
    }

    public double getSumByWeek(final int week, final String itemName, final Map<Integer, Map<String, Map<Integer, Double>>> weeklyBillItemMap) {
        return Optional.ofNullable(weeklyBillItemMap)
                .map(map -> map.get(week))
                .map(weekMap -> weekMap.get(itemName))
                .map(itemMap -> itemMap.values().stream().mapToDouble(Double::doubleValue).sum())
                .orElse(0.0);
    }

    public List<Integer> getDaysOfWeek(final int weekOfMonth) {
        if (month == null) {
            return new ArrayList<>();
        }

        final YearMonth yearMonth = YearMonth.of(LocalDate.now().getYear(), month);
        final LocalDate firstDayOfMonth = yearMonth.atDay(1);

        List<Integer> daysOfWeek = new ArrayList<>();

        LocalDate firstSunday = firstDayOfMonth;
        while (firstSunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
            firstSunday = firstSunday.plusDays(1);
        }

        if (weekOfMonth == 1) {
            LocalDate currentDay = firstDayOfMonth;
            while (currentDay.getDayOfWeek() != DayOfWeek.SUNDAY && currentDay.getMonthValue() == month.getValue()) {
                daysOfWeek.add(currentDay.getDayOfMonth());
                currentDay = currentDay.plusDays(1);
            }
        } else {
            LocalDate firstDayOfWeek = firstSunday.plusWeeks(weekOfMonth - 2);

            for (int i = 0; i < 7; i++) {
                LocalDate day = firstDayOfWeek.plusDays(i);

                if (day.getMonthValue() != month.getValue()) {
                    break;
                }

                daysOfWeek.add(day.getDayOfMonth());
            }
        }

        return daysOfWeek;
    }

    private void groupBillItemsWeekly() {
        Map<String, Map<Integer, Double>> billItemMap7to7 = new HashMap<>();
        Map<String, Map<Integer, Double>> billItemMap7to1 = new HashMap<>();
        Map<String, Map<Integer, Double>> billItemMap1to7 = new HashMap<>();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            final BillItem billItem = row.getBillItem();

            final Date billItemDate = billItem.getBill().getCreatedAt();

            if (billItemDate == null || billItem.getItem() == null) {
                continue;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(billItemDate);

            final int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
            final int weekOfMonth = getWeekOfMonth(billItemDate);

            if (hourOfDay >= 19 || hourOfDay < 7) {
                // Between 7 PM to 7 AM
                billItemMap7to7.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(weekOfMonth, billItemMap7to7.get(billItem.getItem().getName()) != null
                                ? billItemMap7to7.get(billItem.getItem().getName()).getOrDefault(weekOfMonth, 0.0) + billItem.getQty() : billItem.getQty());
            } else if (hourOfDay < 13) {
                // Between 7 AM to 1 PM
                billItemMap7to1.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(weekOfMonth, billItemMap7to1.get(billItem.getItem().getName()) != null
                                ? billItemMap7to1.get(billItem.getItem().getName()).getOrDefault(weekOfMonth, 0.0) + billItem.getQty() : billItem.getQty());
            } else {
                // Between 1 PM to 7 PM
                billItemMap1to7.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(weekOfMonth, billItemMap1to7.get(billItem.getItem().getName()) != null
                                ? billItemMap1to7.get(billItem.getItem().getName()).getOrDefault(weekOfMonth, 0.0) + billItem.getQty() : billItem.getQty());

            }
        }

        setGroupedBillItemsWeeklyValues7to7(billItemMap7to7);
        setGroupedBillItemsWeeklyValues7to1(billItemMap7to1);
        setGroupedBillItemsWeeklyValues1to7(billItemMap1to7);

        Map<Integer, Map<String, Map<Integer, Double>>> billItemMap = new HashMap<>();
        billItemMap.put(1, billItemMap7to7);
        billItemMap.put(2, billItemMap7to1);
        billItemMap.put(3, billItemMap1to7);

        setGroupedBillItemsWeekly(billItemMap);
    }

    public double getWeeklyGroupedBillValues(final String billItemName, final int weekNumber, final int timeSlot) {
        Map<String, Map<Integer, Double>> billItemMap;

        if (timeSlot == 1) {
            billItemMap = groupedBillItemsWeeklyValues7to7;
        } else if (timeSlot == 2) {
            billItemMap = groupedBillItemsWeeklyValues7to1;
        } else if (timeSlot == 3) {
            billItemMap = groupedBillItemsWeeklyValues1to7;
        } else {
            return 0.0;
        }

        if (billItemMap.containsKey(billItemName)) {
            return billItemMap.get(billItemName).getOrDefault(weekNumber, 0.0);
        } else {
            return 0.0;
        }
    }

    public Double getTotalWeeklyGroupedBillValues(String key, Integer entryKey) {
        Double total = 0.0;
        for (int i = 1; i <= 6; i++) {
            Double value = getWeeklyGroupedBillValues(key, i, entryKey);
            if (value != null) {
                total += value;
            }
        }

        return total;
    }

    public static int getWeekOfMonth(final Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);

        return calendar.get(Calendar.WEEK_OF_MONTH);
    }

    private ReportTemplateRowBundle generateWeeklyBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().isEmpty()) {
            jpql += "AND ((bill.billPackege.name) like :itemName ) ";
            parameters.put("itemName", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().isEmpty()) {
            jpql += "AND ((bill.deptId) like :billNo ) ";
            parameters.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (item != null) {
            jpql += "AND billItem.item = :item ";
            parameters.put("item", item);
        }

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        LocalDate firstDayOfMonth = LocalDate.of(LocalDate.now().getYear(), month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());
        Date fromDate = Date.from(firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(lastDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateInvoiceAndReportSerialWiseReport() {
        System.out.println("generateInvoiceAndReportSerialWiseReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<PaymentMethod> paymentMethods = new ArrayList<>();
        if (paymentMethod != null) {
            paymentMethods.add(paymentMethod);
        } else {
            addAllPaymentMethods(paymentMethods);
        }

        List<BillTypeAtomic> opdBts = new ArrayList<>();

        opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        opdBts.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
        opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
        opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);

        bundle.setName("Invoice and Receipt Report Bill Items");
        bundle.setBundleType("billItemList");
        bundle = generateBillItems(opdBts, paymentMethods);
        bundle.calculateTotalByBillItems();
    }

    private void addAllPaymentMethods(List<PaymentMethod> paymentMethods) {
        paymentMethods.add(PaymentMethod.Cash);
        paymentMethods.add(PaymentMethod.Card);
        paymentMethods.add(PaymentMethod.Agent);
        paymentMethods.add(PaymentMethod.Cheque);
        paymentMethods.add(PaymentMethod.Slip);
        paymentMethods.add(PaymentMethod.Credit);
        paymentMethods.add(PaymentMethod.PatientDeposit);
        paymentMethods.add(PaymentMethod.MultiplePaymentMethods);
    }

    private ReportTemplateRowBundle generateBillItems(List<BillTypeAtomic> bts, List<PaymentMethod> billPaymentMethods) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (visitType != null && (visitType.equalsIgnoreCase("IP") || visitType.equalsIgnoreCase("OP"))) {
            jpql += "AND bill.ipOpOrCc = :type ";
            parameters.put("type", visitType);
        }

        if (billPaymentMethods != null) {
            jpql += "AND bill.paymentMethod in :bpms ";
            parameters.put("bpms", billPaymentMethods);
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().isEmpty()) {
            jpql += "AND ((bill.billPackege.name) like :itemName ) ";
            parameters.put("itemName", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().isEmpty()) {
            jpql += "AND ((bill.deptId) like :billNo ) ";
            parameters.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (item != null) {
            jpql += "AND billItem.item = :item ";
            parameters.put("item", item);
        }

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY bill";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateRouteAnalysisReport() {
        System.out.println("generateRouteAnalysisReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();

        opdBts.add(BillTypeAtomic.CC_BILL);
        opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
        opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);

        System.out.println("bill items");

        bundle.setName("Route Analysis Bill Items");
        bundle.setBundleType("billItemList");

        bundle = generateCollectingCenterWiseBillItems(opdBts);

        if (reportType.equalsIgnoreCase("detail")) {
            groupCollectingCenterWiseBillsMonthly();
        } else {
            groupRouteWiseBillsMonthly();
        }
    }

    private void groupRouteWiseBillsMonthly() {
        Map<Route, Map<YearMonth, Bill>> map = new HashMap<>();
        List<YearMonth> yearMonths = new ArrayList<>();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            Bill bill = row.getBill();

            final Calendar cal = Calendar.getInstance();
            cal.setTime(bill.getCreatedAt());

            final YearMonth yearMonth = YearMonth.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);

            if (!yearMonths.contains(yearMonth)) {
                yearMonths.add(yearMonth);
            }

            Map<YearMonth, Bill> monthMap;
            if (map.containsKey(bill.getCollectingCentre().getRoute())) {
                monthMap = map.get(bill.getCollectingCentre().getRoute());

                if (monthMap.containsKey(yearMonth)) {
                    Bill existingBill = monthMap.get(yearMonth);
                    existingBill.setTotalHospitalFee(existingBill.getTotalHospitalFee() + bill.getTotalHospitalFee());
                    existingBill.setQty(existingBill.getQty() + bill.getQty());
                } else {
                    monthMap.put(yearMonth, bill);
                }
            } else {
                monthMap = new HashMap<>();
                monthMap.put(yearMonth, bill);
            }

            map.put(bill.getCollectingCentre().getRoute(), monthMap);
        }

        setGroupedRouteWiseBillsMonthly(map);
        setYearMonths(yearMonths);
    }

    private void groupCollectingCenterWiseBillsMonthly() {
        Map<Institution, Map<YearMonth, Bill>> map = new HashMap<>();
        List<YearMonth> yearMonths = new ArrayList<>();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            Bill bill = row.getBill();

            final Calendar cal = Calendar.getInstance();
            cal.setTime(bill.getCreatedAt());

            final YearMonth yearMonth = YearMonth.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);

            if (!yearMonths.contains(yearMonth)) {
                yearMonths.add(yearMonth);
            }

            Map<YearMonth, Bill> monthMap;
            if (map.containsKey(bill.getCollectingCentre())) {
                monthMap = map.get(bill.getCollectingCentre());

                if (monthMap.containsKey(yearMonth)) {
                    Bill existingBill = monthMap.get(yearMonth);
                    existingBill.setTotalHospitalFee(existingBill.getTotalHospitalFee() + bill.getTotalHospitalFee());
                    existingBill.setQty(existingBill.getQty() + bill.getQty());
                } else {
                    monthMap.put(yearMonth, bill);
                }

            } else {
                monthMap = new HashMap<>();
                monthMap.put(yearMonth, bill);

            }
            map.put(bill.getCollectingCentre(), monthMap);
        }

        setGroupedCollectingCenterWiseBillsMonthly(map);
        setYearMonths(yearMonths);
    }

    public double getCollectionCenterWiseTotalSampleCount(YearMonth yearmonth) {
        double total = 0;
        for (Map.Entry<Institution, Map<YearMonth, Bill>> entry : groupedCollectingCenterWiseBillsMonthly.entrySet()) {
            Bill summary = entry.getValue().get(yearmonth);
            if (summary != null) {
                total += summary.getQty();
            }
        }
        return total;
    }

    public double getCollectionCenterWiseTotalServiceAmount(YearMonth yearmonth) {
        double total = 0;
        for (Map.Entry<Institution, Map<YearMonth, Bill>> entry : groupedCollectingCenterWiseBillsMonthly.entrySet()) {
            Bill summary = entry.getValue().get(yearmonth);
            if (summary != null) {
                total += summary.getTotalHospitalFee();
            }
        }
        return total;
    }

    public double calculateCollectionCenterWiseBillCount(YearMonth yearmonth) {
        double total = 0;
        for (Map.Entry<Institution, Map<YearMonth, Bill>> entry : groupedCollectingCenterWiseBillsMonthly.entrySet()) {
            Bill summary = entry.getValue().get(yearmonth);
            if (summary != null) {
                total += 1;
            }
        }
        return total;
    }

    public double calculateRouteWiseBillCount(YearMonth yearmonth) {
        double total = 0;
        for (Map.Entry<Route, Map<YearMonth, Bill>> entry : groupedRouteWiseBillsMonthly.entrySet()) {
            Bill summary = entry.getValue().get(yearmonth);
            if (summary != null) {
                total += 1;
            }
        }
        return total;
    }

    public double calculateRouteWiseTotalSampleCount(YearMonth yearmonth) {
        double total = 0;
        for (Map.Entry<Route, Map<YearMonth, Bill>> entry : groupedRouteWiseBillsMonthly.entrySet()) {
            Bill summary = entry.getValue().get(yearmonth);
            if (summary != null) {
                total += summary.getQty();
            }
        }
        return total;
    }

    public double calculateRouteWiseTotalServiceAmount(YearMonth yearmonth) {
        double total = 0;
        for (Map.Entry<Route, Map<YearMonth, Bill>> entry : groupedRouteWiseBillsMonthly.entrySet()) {
            Bill summary = entry.getValue().get(yearmonth);
            if (summary != null) {
                total += summary.getTotalHospitalFee();
            }
        }
        return total;
    }

    public Map<YearMonth, Double> getSampleCountChartData() {
        Map<YearMonth, Double> data = new HashMap<>();

        if (reportType.equalsIgnoreCase("detail")) {
            for (YearMonth yearMonth : yearMonths) {
                data.put(yearMonth, getCollectionCenterWiseTotalSampleCount(yearMonth) / calculateCollectionCenterWiseBillCount(yearMonth));
            }
        } else {
            for (YearMonth yearMonth : yearMonths) {
                data.put(yearMonth, calculateRouteWiseTotalSampleCount(yearMonth) / calculateRouteWiseBillCount(yearMonth));
            }
        }

        return data;
    }

    public Map<YearMonth, Double> getServiceAmountChartData() {
        Map<YearMonth, Double> data = new HashMap<>();

        if (reportType.equalsIgnoreCase("detail")) {
            for (YearMonth yearMonth : yearMonths) {
                data.put(yearMonth, getCollectionCenterWiseTotalServiceAmount(yearMonth) / calculateCollectionCenterWiseBillCount(yearMonth));
            }
        } else {
            for (YearMonth yearMonth : yearMonths) {
                data.put(yearMonth, calculateRouteWiseTotalServiceAmount(yearMonth) / calculateRouteWiseBillCount(yearMonth));
            }
        }

        return data;
    }

    public ReportTemplateRowBundle generateCollectingCenterWiseBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br ";
        parameters.put("br", true);
        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }

        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }

        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (route != null) {
            jpql += "AND bill.collectingCentre.route = :route ";
            parameters.put("route", route);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", CommonFunctions.getEndOfDay(toDate));

        jpql += "GROUP BY bill";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateCollectionCenterWiseInvoiceListReport() {
        System.out.println("generateCollectionCenterWiseInvoiceListReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();

        opdBts.add(BillTypeAtomic.CC_BILL);
        opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
        opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);
        // If transaction add other CC types

        System.out.println("bills");

        bundle.setName("Package Bills");
        bundle.setBundleType("billList");

        bundle = generateCollectingCenterWiseBills(opdBts);
        bundle.calculateTotalByHospitalFee();
        bundle.calculateTotalCCFee();
    }

    public ReportTemplateRowBundle generateCollectingCenterWiseBills(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br ";
        parameters.put("br", true);
        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY bill";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBill();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateDebtorBalanceReport(final boolean onlyDueBills) {
        if (visitType == null || visitType.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a visit type");
            return;
        }

        List<PaymentMethod> paymentMethods = new ArrayList<>();
        if (methodType.equalsIgnoreCase("Credit")) {
            paymentMethods.add(PaymentMethod.Credit);
        } else if (methodType.equalsIgnoreCase("NonCredit")) {
            paymentMethods.add(PaymentMethod.Cash);
        } else {
            addAllPaymentMethods(paymentMethods);
        }

        System.out.println("generateDebtorBalanceReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();
        bundle = new ReportTemplateRowBundle();

        if (visitType.equalsIgnoreCase("IP")) {
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
            //TODO: Add other needed bill types

        } else if (visitType.equalsIgnoreCase("OP")) {
            opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
        }

        bundle.setName("Bills");
        bundle.setBundleType("billList");

        bundle = generateDebtorBalanceReportBills(opdBts, paymentMethods, onlyDueBills);

        bundle.calculateTotalByBills(visitType.equalsIgnoreCase("OP"));
        bundle.calculateTotalBalance(visitType.equalsIgnoreCase("OP"));
        bundle.calculateTotalSettledAmountByPatients(visitType.equalsIgnoreCase("OP"));
        bundle.calculateTotalSettledAmountBySponsors(visitType.equalsIgnoreCase("OP"));
    }

    public ReportTemplateRowBundle generateDebtorBalanceReportBills(List<BillTypeAtomic> bts, List<PaymentMethod> billPaymentMethods,
                                                                    boolean onlyDueBills) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.creditCompany is not null ";
        parameters.put("br", true);
        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (onlyDueBills) {
            jpql += "AND bill.balance > 0 ";
        }

        if (billPaymentMethods != null) {
            jpql += "AND bill.paymentMethod in :bpms ";
            parameters.put("bpms", billPaymentMethods);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && admissionType != null)) {
            jpql += "AND bill.patientEncounter.admissionType = :type ";
            parameters.put("type", admissionType);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && roomCategory != null)) {
            jpql += "AND bill.patientEncounter.currentPatientRoom.roomFacilityCharge.roomCategory = :category ";
            parameters.put("category", roomCategory);
        }

        if (institution != null) {
            jpql += "AND bill.creditCompany = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY bill";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBill();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generatePaymentSettlementReport() {
        if (visitType == null || visitType.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a visit type");
            return;
        }

        System.out.println("generatePaymentSettlementReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();
        bundle = new ReportTemplateRowBundle();

        if (visitType.equalsIgnoreCase("IP")) {
            opdBts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED);
            opdBts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION);
        } else if (visitType.equalsIgnoreCase("OP")) {
            opdBts.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
            opdBts.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_CANCELLATION);
        }

        bundle.setName("Bills");
        bundle.setBundleType("billList");

        if (reportType.equalsIgnoreCase("summary")) {
            bundle = generateReportBill(opdBts, null);
            bundle.calculateTotalByRefBills(visitType.equalsIgnoreCase("OP"));
        } else {
            bundle = generateReportBillItems(opdBts, null);
            bundle.calculateTotalByReferenceBills(visitType.equalsIgnoreCase("OP"));
        }


    }

    public ReportTemplateRowBundle generateReportBillItems(List<BillTypeAtomic> bts, List<PaymentMethod> billPaymentMethods) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (billPaymentMethods != null) {
            jpql += "AND bill.paymentMethod in :bpms ";
            parameters.put("bpms", billPaymentMethods);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && admissionType != null)) {
            jpql += "AND billItem.patientEncounter.admissionType = :type ";
            parameters.put("type", admissionType);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && roomCategory != null)) {
            jpql += "AND bill.patientEncounter.currentPatientRoom.roomFacilityCharge.roomCategory = :category ";
            parameters.put("category", roomCategory);
        }

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (creditCompany != null) {
            if (visitType != null && visitType.equalsIgnoreCase("OP")) {
                jpql += "AND billItem.referenceBill.creditCompany = :creditC ";
            } else if (visitType != null && visitType.equalsIgnoreCase("IP")) {
                jpql += "AND billItem.bill.creditCompany = :creditC ";
            }

            parameters.put("creditC", creditCompany);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public ReportTemplateRowBundle generateReportBill(List<BillTypeAtomic> bts, List<PaymentMethod> billPaymentMethods) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (billPaymentMethods != null) {
            jpql += "AND bill.paymentMethod in :bpms ";
            parameters.put("bpms", billPaymentMethods);
        }

//        if (visitType != null && (visitType.equalsIgnoreCase("IP") && admissionType != null)) {
//            jpql += "AND billItem.patientEncounter.admissionType = :type ";
//            parameters.put("type", admissionType);
//        }
//
//        if (visitType != null && (visitType.equalsIgnoreCase("IP") && roomCategory != null)) {
//            jpql += "AND bill.patientEncounter.currentPatientRoom.roomFacilityCharge.roomCategory = :category ";
//            parameters.put("category", roomCategory);
//        }

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (creditCompany != null) {
            if (visitType != null && visitType.equalsIgnoreCase("OP")) {
                jpql += "AND bill.creditCompany = :creditC ";
            } else if (visitType != null && visitType.equalsIgnoreCase("IP")) {
                jpql += "AND bill.creditCompany = :creditC ";
            }

            parameters.put("creditC", creditCompany);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY bill";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateCollectionCenterBookWiseDetailReport() {
        System.out.println("generateCollectionCenterBookWiseDetailReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();
        bundle = new ReportTemplateRowBundle();

        opdBts.add(BillTypeAtomic.CC_BILL);
        opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);
        opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
//        opdBts.add(BillTypeAtomic.CC_PAYMENT_RECEIVED_BILL);

        bundle.setName("Bills");
        bundle.setBundleType("billList");

        bundle = generateCollectionCenterBookWiseBills(opdBts);
    }

    public ReportTemplateRowBundle generateCollectionCenterBookWiseBills(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br ";

        parameters.put("br", true);
        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (institution != null) {
            jpql += "AND bill.creditCompany = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (cashBookNumber != null && !cashBookNumber.trim().isEmpty()) {
            jpql += "AND bill.referenceNumber LIKE :cbn ";
            parameters.put("cbn", "%" + cashBookNumber + "%");
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY bill";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBill();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateCollectionCenterBillWiseDetailReport() {
        System.out.println("generateCollectionCenterBillWiseDetailReport = " + this);

        if (collectingCentre == null) {
            JsfUtil.addErrorMessage("Please select an Agent");
            return;
        }

        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();

        opdBts.add(BillTypeAtomic.CC_BILL);
        opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
        opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);

        System.out.println("bill items");

        bundle.setName("Route Analysis Bill Items");
        bundle.setBundleType("billItemList");

        bundle = generateCollectingCenterBillWiseBillItems(opdBts);

        if (reportType.equalsIgnoreCase("Report")) {
            groupBillItems();
        } else {
            bundle.calculateTotalHospitalFeeByBillItems();
            bundle.calculateTotalByBillItems();
            bundle.calculateTotalStaffFeeByBillItems();
        }
    }

    private void groupBillItems() {
        Map<String, List<BillItem>> billItemMap = new HashMap<>();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            BillItem billItem1 = row.getBillItem();

            if (billItem1.getBill() == null || billItem1.getBill().getDeptId() == null) {
                continue;
            }

            if (billItemMap.containsKey(billItem1.getBill().getDeptId())) {
                billItemMap.get(billItem1.getBill().getDeptId()).add(billItem1);
            } else {
                List<BillItem> billItems = new ArrayList<>();
                billItems.add(billItem1);
                billItemMap.put(billItem1.getBill().getDeptId(), billItems);
            }
        }

        Map<String, List<BillItem>> sortedBillItemMap = billItemMap.entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().get(0).getBill().getCreatedAt()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        bundle.setGroupedBillItems(sortedBillItemMap);
    }

    // Added methods to calculate totals manually since total is not stored in db for old records correctly
    public Double calculateTotalHospitalFeeByBillItems(final List<BillItem> billItems) {
        double totalHospitalFee = 0;

        for (BillItem billItem : billItems) {
            totalHospitalFee += billItem.getHospitalFee();
        }

        return totalHospitalFee;
    }

    public Double calculateTotalStaffFeeByBillItems(final List<BillItem> billItems) {
        double totalStaffFee = 0;

        for (BillItem billItem : billItems) {
            totalStaffFee += billItem.getStaffFee();
        }

        return totalStaffFee;
    }

    public Double calculateTotalCollectionCenterFeeByBillItems(final List<BillItem> billItems) {
        double totalecectionCenterFee = 0;

        for (BillItem billItem : billItems) {
            totalecectionCenterFee += billItem.getCollectingCentreFee();
        }

        return totalecectionCenterFee;
    }

    public Double calculateTotalNetValueByBillItems(final List<BillItem> billItems) {
        double totalNetValue = 0;

        for (BillItem billItem : billItems) {
            totalNetValue += billItem.getNetValue();
        }

        return totalNetValue;
    }

    //Correct
    public ReportTemplateRowBundle generateCollectingCenterBillWiseBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (institution != null) {
            jpql += "AND bill.toInstitution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND (bill.toDepartment = :dep or bill.department = :dep) ";
            parameters.put("dep", department);
        }

        if (site != null) {
            jpql += "AND (bill.toDepartment.site = :site or bill.department.site = :site)";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (creditCompany != null) {
            jpql += "AND bill.creditCompany = :cc ";
            parameters.put("cc", creditCompany);
        }

        if (invoiceNumber != null && !invoiceNumber.trim().isEmpty()) {
            jpql += "AND bill.deptId = :in ";
            parameters.put("in", invoiceNumber);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateCreditInvoiceDispatchReport() {
        if (visitType == null || visitType.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a visit type");
            return;
        }

        System.out.println("generateCreditInvoiceDispatchReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();
        bundle = new ReportTemplateRowBundle();

        if (visitType.equalsIgnoreCase("IP")) {
            opdBts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED);
            opdBts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION);
        } else if (visitType.equalsIgnoreCase("OP")) {
            opdBts.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
            opdBts.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_CANCELLATION);
        } else {
            opdBts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED);
            opdBts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION);
            opdBts.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
            opdBts.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_CANCELLATION);
        }

        bundle.setName("Bills");
        bundle.setBundleType("billList");

        bundle = generateCreditInvoiceDispatchBillItems(opdBts, null);

        bundle.calculateTotalByBills(visitType.equalsIgnoreCase("OP"));
        bundle.calculateTotalBalance(visitType.equalsIgnoreCase("OP"));
        bundle.calculateTotalSettledAmountByPatients(visitType.equalsIgnoreCase("OP"));
        bundle.calculateTotalSettledAmountBySponsors(visitType.equalsIgnoreCase("OP"));
    }

    public ReportTemplateRowBundle generateCreditInvoiceDispatchBillItems(List<BillTypeAtomic> bts, List<PaymentMethod> billPaymentMethods) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (billPaymentMethods != null) {
            jpql += "AND bill.paymentMethod in :bpms ";
            parameters.put("bpms", billPaymentMethods);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && admissionType != null)) {
            jpql += "AND bill.patientEncounter.admissionType = :type ";
            parameters.put("type", admissionType);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && roomCategory != null)) {
            jpql += "AND bill.patientEncounter.currentPatientRoom.roomFacilityCharge.roomCategory = :category ";
            parameters.put("category", roomCategory);
        }

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (creditCompany != null) {
            jpql += "AND bill.creditCompany = :cc ";
            parameters.put("cc", creditCompany);
        }

        if ("notSettled".equalsIgnoreCase(settlementStatus)) {
            jpql += "AND (billItem.referenceBill.netTotal + billItem.referenceBill.vat + billItem.referenceBill.paidAmount) <> 0 ";
        } else if ("settled".equalsIgnoreCase(settlementStatus)) {
            jpql += "AND (billItem.referenceBill.netTotal + billItem.referenceBill.vat + billItem.referenceBill.paidAmount) = 0 ";
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void externalLaboratoryWorkloadReport() {
        if (visitType == null || visitType.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a visit type");
            return;
        }

        System.out.println("generateCreditInvoiceDispatchReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();
        bundle = new ReportTemplateRowBundle();

        if (visitType != null && visitType.equalsIgnoreCase("IP")) {
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
        }
        if (visitType != null && visitType.equalsIgnoreCase("OP")) {
            opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        }
        if (visitType != null && visitType.equalsIgnoreCase("CC")) {
            opdBts.add(BillTypeAtomic.CC_BILL);
            opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
            opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);
        }

        bundle.setName("Bills");
        bundle.setBundleType("billList");

        if (reportType.equalsIgnoreCase("detail")) {
            bundle = generateExternalLaboratoryWorkloadBillItems(opdBts);

            bundle.calculateTotalByBillItemsNetTotal();
        } else {
            bundle = generateExternalLaboratoryWorkloadSummaryBillItems(opdBts);
        }
    }

    private ReportTemplateRowBundle generateExternalLaboratoryWorkloadBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "LEFT JOIN PatientInvestigation pi ON pi.billItem = billItem "
                + "WHERE bill.billTypeAtomic IN :bts ";
//        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
//                + "FROM PatientInvestigation pi "
//                + "JOIN pi.billItem billItem "
//                + "JOIN billItem.bill bill "
//                + "WHERE pi.retired=false "
//                + " AND billItem.retired=false "
//                + " AND bill.retired=false "
//                + " AND bill.billTypeAtomic in :bts ";

        parameters.put("bts", bts);

        if (visitType != null) {
            if (visitType.equalsIgnoreCase("IP") || visitType.equalsIgnoreCase("CC")) {
                jpql += "AND bill.ipOpOrCc = :type ";
                parameters.put("type", visitType);
            } else if (visitType.equalsIgnoreCase("OP")) {
                jpql += "AND (bill.ipOpOrCc = :type OR bill.ipOpOrCc IS NULL) ";
                parameters.put("type", visitType);
            }
        }

        if (staff != null) {
            jpql += "AND billItem.patientInvestigation.barcodeGeneratedBy.webUserPerson.name = :staff ";
            parameters.put("staff", staff.getPerson().getName());
        }

        if (item != null) {
            jpql += "AND billItem.item = :item ";
            parameters.put("item", item);
        }

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (route != null) {
            jpql += "AND bill.collectingCentre.route = :route ";
            parameters.put("route", route);
        }

        if (referingDoctor != null) {
            jpql += "AND billItem.bill.referredBy = :rd ";
            parameters.put("rd", referingDoctor);
        }

        if (mrnNo != null && !mrnNo.trim().isEmpty()) {
            jpql += "AND billItem.bill.patient.phn LIKE :phn ";
            parameters.put("phn", mrnNo + "%");
        }

        if (category != null) {
            jpql += "AND billItem.item.department.id = :cat ";
            parameters.put("cat", category.getId());
        }

        if (investigation != null) {
            jpql += "AND billItem.item = :code ";
            parameters.put("code", investigation);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpqlWithoutCache(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    private ReportTemplateRowBundle generateExternalLaboratoryWorkloadSummaryBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("bts", bts);
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem.item.name, SUM(billItem.qty), billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "LEFT JOIN PatientInvestigation pi ON pi.billItem = billItem "
                + "WHERE bill.billTypeAtomic IN :bts "
                + "AND bill.createdAt BETWEEN :fd AND :td ";

        if (visitType != null) {
            if (visitType.equalsIgnoreCase("IP") || visitType.equalsIgnoreCase("CC")) {
                jpql += "AND bill.ipOpOrCc = :type ";
                parameters.put("type", visitType);
            } else if (visitType.equalsIgnoreCase("OP")) {
                jpql += "AND (bill.ipOpOrCc = :type OR bill.ipOpOrCc IS NULL) ";
                parameters.put("type", visitType);
            }
        }

        if (staff != null) {
            jpql += "AND billItem.patientInvestigation.barcodeGeneratedBy.webUserPerson.name = :staff ";
            parameters.put("staff", staff.getPerson().getName());
        }

        if (item != null) {
            jpql += "AND billItem.patientInvestigation.investigation.name = :item ";
            parameters.put("item", item.getName());
        }

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (route != null) {
            jpql += "AND bill.collectingCentre.route = :route ";
            parameters.put("route", route);
        }

        if (referingDoctor != null) {
            jpql += "AND billItem.bill.referredBy = :rd ";
            parameters.put("rd", referingDoctor);
        }

        if (mrnNo != null && !mrnNo.trim().isEmpty()) {
            jpql += "AND billItem.bill.patient.phn LIKE :phn ";
            parameters.put("phn", mrnNo + "%");
        }

        if (category != null) {
            jpql += "AND billItem.patientInvestigation.investigation.category.id = :cat ";
            parameters.put("cat", category.getId());
        }

        if (investigationCode != null) {
            jpql += "AND billItem.patientInvestigation.investigation.code = :code ";
            parameters.put("code", investigationCode.getCode());
        }

        jpql += "GROUP BY billItem";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);
        createSummaryRows(rs);
        rs.removeIf(row -> row.getRowValue() == 0.0);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    private void createSummaryRows(final List<ReportTemplateRow> rs) {
        Map<String, ReportTemplateRow> reportRowMap = new HashMap<>();

        for (ReportTemplateRow row : rs) {
            if (row.getRowValue() == 0.0) {
                continue;
            }

            if (row.getBillItem().getNetValue() < 0 && row.getRowValue() > 0) {
                row.setRowValue(-row.getRowValue());
            }

            String rowKey = row.getCategoryName();

            reportRowMap.merge(rowKey, row, (existingRow, newRow) -> {
                existingRow.setRowValue(existingRow.getRowValue() + newRow.getRowValue());
                return existingRow;
            });
        }

        rs.clear();
        rs.addAll(reportRowMap.values());
    }

    public void generateOpdAndInwardDueReport() {
        if (visitType == null || visitType.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a visit type");
            return;
        }

        System.out.println("generatePaymentSettlementReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();
        bundle = new ReportTemplateRowBundle();

        if (visitType.equalsIgnoreCase("IP")) {
//            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
//            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
//            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
//            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
//            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
        } else if (visitType.equalsIgnoreCase("OP")) {
//            opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
//            opdBts.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
//            opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
        }

        bundle.setName("Bills");
        bundle.setBundleType("billList");

        bundle = generateOpdAndInwardDueBills(opdBts);

        groupBills();
    }

    public void exportOpdAndInwardOPToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=OPD_AND_INWARD.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("OPD and Inward Report");
            int rowIndex = 0;

            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");

            XSSFCellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("S. No");
            headerRow.createCell(1).setCellValue("Invoice Date");
            headerRow.createCell(2).setCellValue("Invoice No");
            headerRow.createCell(3).setCellValue("Customer Reference No");
            headerRow.createCell(4).setCellValue("MRNO");
            headerRow.createCell(5).setCellValue("Patient Name");
            headerRow.createCell(6).setCellValue("Gross Amt");
            headerRow.createCell(7).setCellValue("Disc Amt");
            headerRow.createCell(8).setCellValue("Net Amt");
            headerRow.createCell(9).setCellValue("Patient Share");
            headerRow.createCell(10).setCellValue("Sponsor Share");
            headerRow.createCell(11).setCellValue("Due Amt");

            int serialNumber = 1;
            for (Map.Entry<Institution, List<Bill>> entrySet : getBundle().getGroupedBillItemsByInstitution().entrySet()) {
                Institution institution = entrySet.getKey();
                List<Bill> bills = entrySet.getValue();

                Row institutionRow = sheet.createRow(rowIndex++);
                institutionRow.createCell(0).setCellValue(institution.getName());

                for (Bill bill : bills) {
                    Row dataRow = sheet.createRow(rowIndex++);
                    dataRow.createCell(0).setCellValue(serialNumber++);
                    String formattedDate = dateFormatter.format(bill.getCreatedAt());
                    dataRow.createCell(1).setCellValue(formattedDate);
                    dataRow.createCell(2).setCellValue(bill.getDeptId());
                    dataRow.createCell(3).setCellValue(bill.getReferenceNumber());
                    dataRow.createCell(4).setCellValue(bill.getPatient().getPhn());
                    dataRow.createCell(5).setCellValue(bill.getPatient().getPerson().getName());
                    dataRow.createCell(6).setCellValue(bill.getBillTotal());
                    dataRow.createCell(7).setCellValue(bill.getDiscount());
                    dataRow.createCell(8).setCellValue(bill.getNetTotal());
                    dataRow.createCell(9).setCellValue(bill.getSettledAmountByPatient());
                    dataRow.createCell(10).setCellValue(bill.getSettledAmountBySponsor());
                    dataRow.createCell(11).setCellValue(bill.getNetTotal() - bill.getSettledAmountBySponsor() - bill.getSettledAmountByPatient());

                    dataRow.getCell(6).setCellStyle(amountStyle);
                    dataRow.getCell(7).setCellStyle(amountStyle);
                    dataRow.getCell(8).setCellStyle(amountStyle);
                    dataRow.getCell(9).setCellStyle(amountStyle);
                    dataRow.getCell(10).setCellStyle(amountStyle);
                    dataRow.getCell(11).setCellStyle(amountStyle);
                }

                Row institutionTotalRow = sheet.createRow(rowIndex++);
                institutionTotalRow.createCell(5).setCellValue("Sub Total");
                institutionTotalRow.createCell(6).setCellValue(calculateGrossAmountSubTotalByBills(bills));
                institutionTotalRow.createCell(7).setCellValue(calculateDiscountSubTotalByBills(bills));
                institutionTotalRow.createCell(8).setCellValue(calculateNetAmountSubTotalByBills(bills));
                institutionTotalRow.createCell(9).setCellValue(calculatePatientShareSubTotalByBills(bills));
                institutionTotalRow.createCell(10).setCellValue(calculateSponsorShareSubTotalByBills(bills));
                institutionTotalRow.createCell(11).setCellValue(calculateDueAmountSubTotalByBills(bills));

                institutionTotalRow.getCell(6).setCellStyle(amountStyle);
                institutionTotalRow.getCell(7).setCellStyle(amountStyle);
                institutionTotalRow.getCell(8).setCellStyle(amountStyle);
                institutionTotalRow.getCell(9).setCellStyle(amountStyle);
                institutionTotalRow.getCell(10).setCellStyle(amountStyle);
                institutionTotalRow.getCell(11).setCellStyle(amountStyle);
            }

            Row footerRow = sheet.createRow(rowIndex++);
            footerRow.createCell(5).setCellValue("Net Total");
            footerRow.createCell(6).setCellValue(calculateGrossAmountNetTotal());
            footerRow.createCell(7).setCellValue(calculateDiscountNetTotal());
            footerRow.createCell(8).setCellValue(calculateNetAmountNetTotal());
            footerRow.createCell(9).setCellValue(calculatePatientShareNetTotal());
            footerRow.createCell(10).setCellValue(calculateSponsorShareNetTotal());
            footerRow.createCell(11).setCellValue(calculateDueAmountNetTotal());

            footerRow.getCell(6).setCellStyle(amountStyle);
            footerRow.getCell(7).setCellStyle(amountStyle);
            footerRow.getCell(8).setCellStyle(amountStyle);
            footerRow.getCell(9).setCellStyle(amountStyle);
            footerRow.getCell(10).setCellStyle(amountStyle);
            footerRow.getCell(11).setCellStyle(amountStyle);

            workbook.write(out);
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportOpdAndInwardOPToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=OPD_AND_INWARD.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("OPD and Inward Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            com.itextpdf.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            com.itextpdf.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            PdfPTable table = new PdfPTable(12);
            table.setWidthPercentage(100);

            float[] columnWidths = {1.5f, 2.5f, 2.5f, 2.5f, 2.5f, 3.0f, 3.0f, 3.0f, 3.0f, 3.0f, 3.0f, 3.0f};
            table.setWidths(columnWidths);

            String[] headers = {"S. No", "Invoice Date", "Invoice No", "Customer Reference No", "MRNO", "Patient Name",
                    "Gross Amt", "Disc Amt", "Net Amt", "Patient Share", "Sponsor Share", "Due Amt"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            int serialNumber = 1;

            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");

            for (Map.Entry<Institution, List<Bill>> entrySet : getBundle().getGroupedBillItemsByInstitution().entrySet()) {
                Institution institution = entrySet.getKey();
                List<Bill> bills = entrySet.getValue();

                PdfPCell institutionCell = new PdfPCell(new Phrase(institution.getName(), boldFont));
                institutionCell.setColspan(12);
                table.addCell(institutionCell);

                for (Bill bill : bills) {
                    table.addCell(new PdfPCell(new Phrase(String.valueOf(serialNumber++), normalFont)));
                    String formattedDate = dateFormatter.format(bill.getCreatedAt());
                    table.addCell(new PdfPCell(new Phrase(formattedDate, normalFont)));
                    table.addCell(new PdfPCell(new Phrase(bill.getDeptId(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(bill.getReferenceNumber(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(bill.getPatient().getPhn(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(bill.getPatient().getPerson().getName(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getBillTotal()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getDiscount()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getNetTotal()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getSettledAmountByPatient()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getSettledAmountBySponsor()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(
                            bill.getNetTotal() - bill.getSettledAmountBySponsor() - bill.getSettledAmountByPatient()), normalFont)));
                }

                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("Sub Total", boldFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateGrossAmountSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateDiscountSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateNetAmountSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculatePatientShareSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateSponsorShareSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateDueAmountSubTotalByBills(bills)), normalFont)));
            }

            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("Net Total", boldFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateGrossAmountNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateDiscountNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateNetAmountNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculatePatientShareNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateSponsorShareNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateDueAmountNetTotal()), normalFont)));

            document.add(table);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportOpdAndInwardIPToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=OPD_AND_INWARD.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("OPD and Inward Report");
            int rowIndex = 0;

            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");

            XSSFCellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("S. No");
            headerRow.createCell(1).setCellValue("BHT No");
            headerRow.createCell(2).setCellValue("Invoice Date");
            headerRow.createCell(3).setCellValue("Invoice No");
            headerRow.createCell(4).setCellValue("Customer Reference No");
            headerRow.createCell(5).setCellValue("MRNO");
            headerRow.createCell(6).setCellValue("Patient Name");
            headerRow.createCell(7).setCellValue("Gross Amt");
            headerRow.createCell(8).setCellValue("Disc Amt");
            headerRow.createCell(9).setCellValue("Net Amt");
            headerRow.createCell(10).setCellValue("Patient Share");
            headerRow.createCell(11).setCellValue("Sponsor Share");
            headerRow.createCell(12).setCellValue("Due Amt");

            int serialNumber = 1;
            for (Map.Entry<Institution, List<Bill>> entrySet : getBundle().getGroupedBillItemsByInstitution().entrySet()) {
                Institution institution = entrySet.getKey();
                List<Bill> bills = entrySet.getValue();

                Row institutionRow = sheet.createRow(rowIndex++);
                institutionRow.createCell(0).setCellValue(institution.getName());

                for (Bill bill : bills) {
                    Row dataRow = sheet.createRow(rowIndex++);
                    dataRow.createCell(0).setCellValue(serialNumber++);
                    dataRow.createCell(1).setCellValue(bill.getPatientEncounter().getBhtNo());
                    String formattedDate = dateFormatter.format(bill.getPatientEncounter().getFinalBill().getCreatedAt());
                    dataRow.createCell(2).setCellValue(formattedDate);
                    dataRow.createCell(3).setCellValue(bill.getPatientEncounter().getFinalBill().getDeptId());
                    dataRow.createCell(4).setCellValue(bill.getPatientEncounter().getFinalBill().getReferenceNumber());
                    dataRow.createCell(5).setCellValue(bill.getPatient().getPhn());
                    dataRow.createCell(6).setCellValue(bill.getPatient().getPerson().getName());
                    Cell grossAmtCell = dataRow.createCell(7);
                    grossAmtCell.setCellValue(bill.getPatientEncounter().getFinalBill().getGrantTotal());
                    grossAmtCell.setCellStyle(amountStyle);
                    Cell discAmtCell = dataRow.createCell(8);
                    discAmtCell.setCellValue(bill.getPatientEncounter().getFinalBill().getDiscount());
                    discAmtCell.setCellStyle(amountStyle);
                    Cell netAmtCell = dataRow.createCell(9);
                    netAmtCell.setCellValue(bill.getPatientEncounter().getFinalBill().getNetTotal());
                    netAmtCell.setCellStyle(amountStyle);
                    Cell patientShareCell = dataRow.createCell(10);
                    patientShareCell.setCellValue(bill.getPatientEncounter().getFinalBill().getSettledAmountByPatient());
                    patientShareCell.setCellStyle(amountStyle);
                    Cell sponsorShareCell = dataRow.createCell(11);
                    sponsorShareCell.setCellValue(bill.getPatientEncounter().getFinalBill().getSettledAmountBySponsor());
                    sponsorShareCell.setCellStyle(amountStyle);
                    Cell dueAmtCell = dataRow.createCell(12);
                    dueAmtCell.setCellValue(bill.getPatientEncounter().getFinalBill().getNetTotal()
                            - bill.getPatientEncounter().getFinalBill().getSettledAmountBySponsor()
                            - bill.getPatientEncounter().getFinalBill().getSettledAmountByPatient());
                    dueAmtCell.setCellStyle(amountStyle);
                }

                Row institutionTotalRow = sheet.createRow(rowIndex++);
                institutionTotalRow.createCell(6).setCellValue("Sub Total");
                Cell subTotalGrossAmtCell = institutionTotalRow.createCell(7);
                subTotalGrossAmtCell.setCellValue(calculateIpGrossAmountSubTotalByBills(bills));
                subTotalGrossAmtCell.setCellStyle(amountStyle);
                Cell subTotalDiscAmtCell = institutionTotalRow.createCell(8);
                subTotalDiscAmtCell.setCellValue(calculateIpDiscountSubTotalByBills(bills));
                subTotalDiscAmtCell.setCellStyle(amountStyle);
                Cell subTotalNetAmtCell = institutionTotalRow.createCell(9);
                subTotalNetAmtCell.setCellValue(calculateIpNetAmountSubTotalByBills(bills));
                subTotalNetAmtCell.setCellStyle(amountStyle);
                Cell subTotalPatientShareCell = institutionTotalRow.createCell(10);
                subTotalPatientShareCell.setCellValue(calculateIpPatientShareSubTotalByBills(bills));
                subTotalPatientShareCell.setCellStyle(amountStyle);
                Cell subTotalSponsorShareCell = institutionTotalRow.createCell(11);
                subTotalSponsorShareCell.setCellValue(calculateIpSponsorShareSubTotalByBills(bills));
                subTotalSponsorShareCell.setCellStyle(amountStyle);
                Cell subTotalDueAmtCell = institutionTotalRow.createCell(12);
                subTotalDueAmtCell.setCellValue(calculateIpDueAmountSubTotalByBills(bills));
                subTotalDueAmtCell.setCellStyle(amountStyle);
            }

            Row footerRow = sheet.createRow(rowIndex++);
            footerRow.createCell(6).setCellValue("Net Total");
            Cell netTotalGrossAmtCell = footerRow.createCell(7);
            netTotalGrossAmtCell.setCellValue(calculateIpGrossAmountNetTotal());
            netTotalGrossAmtCell.setCellStyle(amountStyle);
            Cell netTotalDiscAmtCell = footerRow.createCell(8);
            netTotalDiscAmtCell.setCellValue(calculateIpDiscountNetTotal());
            netTotalDiscAmtCell.setCellStyle(amountStyle);
            Cell netTotalNetAmtCell = footerRow.createCell(9);
            netTotalNetAmtCell.setCellValue(calculateIpNetAmountNetTotal());
            netTotalNetAmtCell.setCellStyle(amountStyle);
            Cell netTotalPatientShareCell = footerRow.createCell(10);
            netTotalPatientShareCell.setCellValue(calculateIpPatientShareNetTotal());
            netTotalPatientShareCell.setCellStyle(amountStyle);
            Cell netTotalSponsorShareCell = footerRow.createCell(11);
            netTotalSponsorShareCell.setCellValue(calculateIpSponsorShareNetTotal());
            netTotalSponsorShareCell.setCellStyle(amountStyle);
            Cell netTotalDueAmtCell = footerRow.createCell(12);
            netTotalDueAmtCell.setCellValue(calculateIpDueAmountNetTotal());
            netTotalDueAmtCell.setCellStyle(amountStyle);

            workbook.write(out);
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportOpdAndInwardIPToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=OPD_AND_INWARD.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("OPD and Inward Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            com.itextpdf.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            com.itextpdf.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            PdfPTable table = new PdfPTable(13);
            table.setWidthPercentage(100);

            float[] columnWidths = {1.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 3.0f, 3.0f, 3.0f, 3.0f, 3.0f, 3.0f, 3.0f};
            table.setWidths(columnWidths);

            String[] headers = {"S. No", "BHT No", "Invoice Date", "Invoice No", "Customer Reference No", "MRNO", "Patient Name",
                    "Gross Amt", "Disc Amt", "Net Amt", "Patient Share", "Sponsor Share", "Due Amt"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            int serialNumber = 1;

            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");

            for (Map.Entry<Institution, List<Bill>> entrySet : getBundle().getGroupedBillItemsByInstitution().entrySet()) {
                Institution institution = entrySet.getKey();
                List<Bill> bills = entrySet.getValue();

                PdfPCell institutionCell = new PdfPCell(new Phrase(institution.getName(), boldFont));
                institutionCell.setColspan(13);
                table.addCell(institutionCell);

                for (Bill bill : bills) {
                    table.addCell(new PdfPCell(new Phrase(String.valueOf(serialNumber++), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(bill.getPatientEncounter().getBhtNo(), normalFont)));
                    String formattedDate = dateFormatter.format(bill.getPatientEncounter().getFinalBill().getCreatedAt());
                    table.addCell(new PdfPCell(new Phrase(formattedDate, normalFont)));
                    table.addCell(new PdfPCell(new Phrase(bill.getPatientEncounter().getFinalBill().getDeptId(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(bill.getPatientEncounter().getFinalBill().getReferenceNumber(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(bill.getPatient().getPhn(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(bill.getPatient().getPerson().getName(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getPatientEncounter().getFinalBill().getGrantTotal()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getPatientEncounter().getFinalBill().getDiscount()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getPatientEncounter().getFinalBill().getNetTotal()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getPatientEncounter().getFinalBill().getSettledAmountByPatient()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getPatientEncounter().getFinalBill().getSettledAmountBySponsor()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getPatientEncounter().getFinalBill().getNetTotal()
                            - bill.getPatientEncounter().getFinalBill().getSettledAmountBySponsor()
                            - bill.getPatientEncounter().getFinalBill().getSettledAmountByPatient()), normalFont)));
                }

                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("Sub Total", boldFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateIpGrossAmountSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateIpDiscountSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateIpNetAmountSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateIpPatientShareSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateIpSponsorShareSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateIpDueAmountSubTotalByBills(bills)), normalFont)));
            }

            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("Net Total", boldFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateIpGrossAmountNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateIpDiscountNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateIpNetAmountNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateIpPatientShareNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateIpSponsorShareNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateIpDueAmountNetTotal()), normalFont)));

            document.add(table);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ReportTemplateRowBundle generateOpdAndInwardDueBills(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.creditCompany is not null ";

        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (paymentMethod != null) {
            List<PaymentMethod> billPaymentMethods = new ArrayList<>();
            billPaymentMethods.add(paymentMethod);

            jpql += "AND bill.paymentMethod in :bpms ";
            parameters.put("bpms", billPaymentMethods);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && admissionType != null)) {
            jpql += "AND bill.patientEncounter.admissionType = :type ";
            parameters.put("type", admissionType);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && roomCategory != null)) {
            jpql += "AND bill.patientEncounter.currentPatientRoom.roomFacilityCharge.roomCategory = :category ";
            parameters.put("category", roomCategory);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && dischargedStatus != null && !dischargedStatus.trim().isEmpty())) {
            if (dischargedStatus.equalsIgnoreCase("notDischarged")) {
                jpql += "AND bill.patientEncounter.discharged = :status ";
                parameters.put("status", false);
            } else if (dischargedStatus.equalsIgnoreCase("discharged")) {
                jpql += "AND bill.patientEncounter.discharged = :status ";
                parameters.put("status", true);
            }
        }

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (creditCompany != null) {
            jpql += "AND bill.creditCompany = :cc ";
            parameters.put("cc", creditCompany);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY bill, bill.creditCompany";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBill();
        b.calculateTotalsWithCredit();
        return b;
    }

    private void groupBills() {
        Map<Institution, List<Bill>> billMap = new HashMap<>();

        if (visitType != null && visitType.equalsIgnoreCase("OP")) {
            for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
                Bill bill1 = row.getBill();

                if (reportType != null && reportType.equalsIgnoreCase("paid")) {
                    if ((bill1.getNetTotal() - bill1.getPaidAmount()) != 0) {
                        continue;
                    }
                }

                if (reportType != null && reportType.equalsIgnoreCase("due")) {
                    if ((bill1.getNetTotal() - bill1.getPaidAmount()) == 0) {
                        continue;
                    }
                }

                if (billMap.containsKey(bill1.getCreditCompany())) {
                    billMap.get(bill1.getCreditCompany()).add(bill1);
                } else {
                    List<Bill> bills = new ArrayList<>();
                    bills.add(bill1);
                    billMap.put(bill1.getCreditCompany(), bills);
                }
            }
        } else if (visitType != null && visitType.equalsIgnoreCase("IP")) {
            for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
                Bill bill1 = row.getBill();

                if (reportType != null && reportType.equalsIgnoreCase("paid")) {
                    if (bill1.getPatientEncounter().getFinalBill().getBalance() != 0) {
                        continue;
                    }
                }

                if (reportType != null && reportType.equalsIgnoreCase("due")) {
                    if (bill1.getPatientEncounter().getFinalBill().getBalance() == 0) {
                        continue;
                    }
                }

                if (billMap.containsKey(bill1.getPatientEncounter().getFinalBill().getCreditCompany())) {
                    billMap.get(bill1.getPatientEncounter().getFinalBill().getCreditCompany()).add(bill1);
                } else {
                    List<Bill> bills = new ArrayList<>();
                    bills.add(bill1);
                    billMap.put(bill1.getPatientEncounter().getFinalBill().getCreditCompany(), bills);
                }
            }
        }

        bundle.setGroupedBillItemsByInstitution(billMap);
    }

    public Double calculateSubTotal() {
        double subTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            subTotal += calculateNetAmountSubTotalByBills(bills);
        }

        return subTotal;
    }

    public Double calculateGrossAmountSubTotalByBills(List<Bill> bills) {
        Double billTotal = 0.0;

        for (Bill bill : bills) {
            billTotal += bill.getBillTotal();
        }

        return billTotal;
    }

    public Double calculatePatientShareSubTotalByBills(List<Bill> bills) {
        Double settledAmountByPatient = 0.0;

        for (Bill bill : bills) {
            settledAmountByPatient += bill.getSettledAmountByPatient();
        }

        return settledAmountByPatient;
    }

    public Double calculateSponsorShareSubTotalByBills(List<Bill> bills) {
        Double settledAmountBySponsor = 0.0;

        for (Bill bill : bills) {
            settledAmountBySponsor += bill.getSettledAmountBySponsor();
        }

        return settledAmountBySponsor;
    }

    public Double calculateDueAmountSubTotalByBills(List<Bill> bills) {
        Double balance = 0.0;

        for (Bill bill : bills) {
            balance += bill.getNetTotal() - bill.getSettledAmountBySponsor() - bill.getSettledAmountByPatient();
        }

        return balance;
    }

    public Double calculateGrossAmountNetTotal() {
        double grossAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            grossAmountNetTotal += calculateGrossAmountSubTotalByBills(bills);
        }

        return grossAmountNetTotal;
    }

    public Double calculateDiscountNetTotal() {
        double discountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            discountNetTotal += calculateDiscountSubTotalByBills(bills);
        }

        return discountNetTotal;
    }

    public Double calculatePatientShareNetTotal() {
        double patientShareNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            patientShareNetTotal += calculatePatientShareSubTotalByBills(bills);
        }

        return patientShareNetTotal;
    }

    public Double calculateDueAmountNetTotal() {
        double dueAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            dueAmountNetTotal += calculateDueAmountSubTotalByBills(bills);
        }

        return dueAmountNetTotal;
    }

    public Double calculateSponsorShareNetTotal() {
        double sponsorShareNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            sponsorShareNetTotal += calculateSponsorShareSubTotalByBills(bills);

        }

        return sponsorShareNetTotal;
    }

    public Double calculateNetAmountSubTotalByBills(List<Bill> bills) {
        Double netTotal = 0.0;

        for (Bill bill : bills) {
            netTotal += bill.getNetTotal();
        }

        return netTotal;
    }

    public Double calculateDiscountSubTotalByBills(List<Bill> bills) {
        Double discount = 0.0;

        for (Bill bill : bills) {
            discount += bill.getDiscount();
        }

        return discount;
    }

    public Double calculateNetAmountNetTotal() {
        double netAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            netAmountNetTotal += calculateNetAmountSubTotalByBills(bills);
        }

        return netAmountNetTotal;
    }

    public void generateDiscountReport() {
        if (visitType == null || visitType.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a visit type");
            return;
        }

        System.out.println("generateDiscountReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();
        bundle = new ReportTemplateRowBundle();

        if (visitType.equalsIgnoreCase("IP")) {
            if (reportType.equalsIgnoreCase("detail")) {
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);
                opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
            } else if (reportType.equalsIgnoreCase("summary")) {
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);
                opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
            }
        } else if (visitType.equalsIgnoreCase("OP")) {
            if (reportType.equalsIgnoreCase("detail")) {
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
                opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
                opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.OPD_BILL_REFUND);
                opdBts.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
            } else if (reportType.equalsIgnoreCase("summary")) {
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
                opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
                opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.OPD_BILL_REFUND);
                opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
            }
        }

        if (reportType.equalsIgnoreCase("detail")) {
            bundle.setName("BillItems");
            bundle.setBundleType("billItemList");

            bundle = generateDiscountBillItems(opdBts);
            bundle.calculateTotalDiscountByBillItems();
        } else if (reportType.equalsIgnoreCase("summary")) {
            bundle.setName("Bills");
            bundle.setBundleType("billList");

            bundle = generateDiscountBills(opdBts);
            bundle.calculateTotalDiscountByBills();
        }
    }

    public double calculateDiscountForOP(final BillItem billItem, final List<ReportTemplateRow> reportTemplateRows) {
        if (!billItem.getItem().isDiscountAllowed()) {
            return 0.0;
        }

        double totalDiscount = billItem.getBill().getDiscount();
        int billItemCount = 0;

        for (ReportTemplateRow row : reportTemplateRows) {
            BillItem item = row.getBillItem();

            if (item.getBill().equals(billItem.getBill()) && item.getItem().isDiscountAllowed()) {
                billItemCount++;
            }
        }

        return billItemCount > 0 ? totalDiscount / billItemCount : 0.0;
    }

    public double calculateDiscountForIP(final BillItem billItem, final List<ReportTemplateRow> reportTemplateRows) {
        double totalDiscount = billItem.getBill().getDiscount();
        int billItemCount = 0;

        for (ReportTemplateRow row : reportTemplateRows) {
            BillItem item = row.getBillItem();

            if (item.getBill().equals(billItem.getBill()) && billItem.getNetValue() > 0.0) {
                billItemCount++;
            }
        }

        return billItemCount > 0 ? totalDiscount / billItemCount : 0.0;
    }

    public ReportTemplateRowBundle generateDiscountBills(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

//        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
//                + "FROM Bill bill "
//                + "WHERE bill.retired <> :br "
//                + "AND bill.paymentScheme is not null ";
        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.discount > 0 ";

        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (paymentMethod != null) {
            List<PaymentMethod> billPaymentMethods = new ArrayList<>();
            billPaymentMethods.add(paymentMethod);

            jpql += "AND bill.paymentMethod in :bpms ";
            parameters.put("bpms", billPaymentMethods);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && admissionType != null)) {
            jpql += "AND bill.patientEncounter.admissionType = :type ";
            parameters.put("type", admissionType);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && roomCategory != null)) {
            jpql += "AND bill.patientEncounter.currentPatientRoom.roomFacilityCharge.roomCategory = :category ";
            parameters.put("category", roomCategory);
        }

        if (discount > 0) {
            jpql += "AND bill.discount = :dis ";
            parameters.put("dis", discount);
        }

        if (remarks != null && !remarks.trim().isEmpty()) {
            jpql += "AND bill.comments LIKE :rem ";
            parameters.put("rem", remarks + "%");
        }

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (creditCompany != null) {
            jpql += "AND bill.creditCompany = :cc ";
            parameters.put("cc", creditCompany);
        }

        if (paymentScheme != null) {
            jpql += "AND bill.paymentScheme = :ps ";
            parameters.put("ps", paymentScheme);
        }

        if (selectedDateType != null && !selectedDateType.trim().isEmpty() && selectedDateType.equalsIgnoreCase("admission")) {
            jpql += "AND bill.patientEncounter.dateOfAdmission BETWEEN :fd AND :td ";
        } else if (selectedDateType != null && !selectedDateType.trim().isEmpty() && selectedDateType.equalsIgnoreCase("discharge")) {
            jpql += "AND bill.patientEncounter.dateOfDischarge BETWEEN :fd AND :td ";
        } else {
            jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        }

        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY bill";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBill();
        b.calculateTotalsWithCredit();
        return b;
    }

    public ReportTemplateRowBundle generateDiscountBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

//        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
//                + "FROM BillItem billItem "
//                + "JOIN billItem.bill bill "
//                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br "
//                + "AND billItem.bill.paymentScheme is not null ";

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br "
                + "AND bill.discount > 0 ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (paymentMethod != null) {
            List<PaymentMethod> billPaymentMethods = new ArrayList<>();
            billPaymentMethods.add(paymentMethod);

            jpql += "AND bill.paymentMethod in :bpms ";
            parameters.put("bpms", billPaymentMethods);
        }

        if (discount > 0) {
            jpql += "AND billItem.bill.discount = :dis ";
            parameters.put("dis", discount);
        }

        if (remarks != null && !remarks.trim().isEmpty()) {
            jpql += "AND billItem.bill.comments LIKE :rem ";
            parameters.put("rem", remarks + "%");
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && admissionType != null)) {
            jpql += "AND bill.patientEncounter.admissionType = :type ";
            parameters.put("type", admissionType);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && roomCategory != null)) {
            jpql += "AND bill.patientEncounter.currentPatientRoom.roomFacilityCharge.roomCategory = :category ";
            parameters.put("category", roomCategory);
        }

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (creditCompany != null) {
            jpql += "AND bill.creditCompany = :cc ";
            parameters.put("cc", creditCompany);
        }

        if (paymentScheme != null) {
            jpql += "AND bill.paymentScheme = :ps ";
            parameters.put("ps", paymentScheme);
        }

        if ("notSettled".equalsIgnoreCase(settlementStatus)) {
            jpql += "AND (billItem.referenceBill.netTotal + billItem.referenceBill.vat + billItem.referenceBill.paidAmount) <> 0 ";
        } else if ("settled".equalsIgnoreCase(settlementStatus)) {
            jpql += "AND (billItem.referenceBill.netTotal + billItem.referenceBill.vat + billItem.referenceBill.paidAmount) = 0 ";
        }

        if (selectedDateType != null && !selectedDateType.trim().isEmpty() && selectedDateType.equalsIgnoreCase("admission")) {
            jpql += "AND bill.patientEncounter.dateOfAdmission BETWEEN :fd AND :td ";
        } else if (selectedDateType != null && !selectedDateType.trim().isEmpty() && selectedDateType.equalsIgnoreCase("discharge")) {
            jpql += "AND bill.patientEncounter.dateOfDischarge BETWEEN :fd AND :td ";
        } else {
            jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        }

        if (visitType.equalsIgnoreCase("IP")) {
            jpql += "AND billItem.netValue > 0 ";
        }

        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        for (ReportTemplateRow row : rs) {
            BillItem billItem = row.getBillItem();

            billItem.setDiscount(visitType.equalsIgnoreCase("OP") ? calculateDiscountForOP(billItem, rs) : calculateDiscountForIP(billItem, rs));
        }

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void exportCollectionCenterBillWiseDetailReportToPDF() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Collection_Center_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);

            document.open();

            document.add(new Paragraph("Collection Center Bill Wise Detail Report",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));

            PdfPTable table = new PdfPTable(7);
            float[] columnWidths = {1f, 1f, 1f, 1f, 1f, 1f, 3f};
            table.setWidths(columnWidths);
            table.setWidthPercentage(100);

            table.addCell("CC Code");
            table.addCell("Leaf No.");
            table.addCell("MRN");
            table.addCell("Patient Name");
            table.addCell("Invoice Date");
            table.addCell("Invoice No");
            table.addCell("Details");

            for (Map.Entry<String, List<BillItem>> entry : bundle.getGroupedBillItems().entrySet()) {
                List<BillItem> billItems = entry.getValue();

                if (billItems == null || billItems.isEmpty()) {
                    table.addCell("N/A");
                    table.addCell("N/A");
                    table.addCell("N/A");
                    table.addCell("N/A");
                    table.addCell("N/A");
                    table.addCell(entry.getKey());
                    PdfPCell emptyCell = new PdfPCell(new Phrase("No Details Available"));
                    emptyCell.setColspan(7);
                    table.addCell(emptyCell);
                    continue;
                }

                BillItem firstItem = billItems.get(0);

                table.addCell(firstItem.getBill().getCollectingCentre().getCode());
                table.addCell(firstItem.getBill().getReferenceNumber());
                table.addCell(firstItem.getBill().getPatient().getPhn());
                table.addCell(firstItem.getBill().getPatient().getPerson().getName());
                table.addCell(firstItem.getBill().getCreatedAt().toString());
                table.addCell(entry.getKey());

                PdfPTable detailsTable = new PdfPTable(5);
                float[] columnWidthsInner = {2f, 1f, 1f, 1f, 1f};
                detailsTable.setWidths(columnWidthsInner);

                detailsTable.addCell("Service Name");
                detailsTable.addCell("Hos Fee");
                detailsTable.addCell("Staff Fee");
                detailsTable.addCell("CC Fee");
                detailsTable.addCell("Net Amount");

                for (BillItem bi : billItems) {
                    detailsTable.addCell(bi.getItem().getName());
                    detailsTable.addCell(String.valueOf(bi.getHospitalFee()));
                    detailsTable.addCell(String.valueOf(bi.getStaffFee()));
                    detailsTable.addCell(String.valueOf(bi.getCollectingCentreFee()));
                    detailsTable.addCell(String.valueOf(bi.getNetValue()));
                }

                detailsTable.addCell("Total");
                detailsTable.addCell(String.valueOf(calculateTotalHospitalFeeByBillItems(billItems)));
                detailsTable.addCell(String.valueOf(calculateTotalStaffFeeByBillItems(billItems)));
                detailsTable.addCell(String.valueOf(calculateTotalCollectionCenterFeeByBillItems(billItems)));
                detailsTable.addCell(String.valueOf(calculateTotalNetValueByBillItems(billItems)));

                PdfPCell nestedCell = new PdfPCell(detailsTable);
                nestedCell.setColspan(7);
                table.addCell(nestedCell);
            }

            document.add(table);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportCollectionCenterBillWiseDetailReportToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Collection_Center_Report.xlsx");

        SimpleDateFormat sdf = new SimpleDateFormat("dd MM yyyy hh:mm:ss a");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Report");
            int rowIndex = 0;

            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("CC Code");
            headerRow.createCell(1).setCellValue("Leaf No.");
            headerRow.createCell(2).setCellValue("MRN");
            headerRow.createCell(3).setCellValue("Patient Name");
            headerRow.createCell(4).setCellValue("Invoice Date");
            headerRow.createCell(5).setCellValue("Invoice No");
            headerRow.createCell(6).setCellValue("Details");

            for (Map.Entry<String, List<BillItem>> entry : bundle.getGroupedBillItems().entrySet()) {
                List<BillItem> billItems = entry.getValue();

                if (billItems == null || billItems.isEmpty()) {
                    Row emptyRow = sheet.createRow(rowIndex++);
                    emptyRow.createCell(0).setCellValue("N/A");
                    emptyRow.createCell(1).setCellValue("N/A");
                    emptyRow.createCell(2).setCellValue("N/A");
                    emptyRow.createCell(3).setCellValue("N/A");
                    emptyRow.createCell(4).setCellValue("N/A");
                    emptyRow.createCell(5).setCellValue(entry.getKey());
                    emptyRow.createCell(6).setCellValue("No Details Available");
                    continue;
                }

                BillItem firstItem = billItems.get(0);
                Row row = sheet.createRow(rowIndex++);

                row.createCell(0).setCellValue(firstItem.getBill().getCollectingCentre().getCode());
                row.createCell(1).setCellValue(firstItem.getBill().getReferenceNumber());
                row.createCell(2).setCellValue(firstItem.getBill().getPatient().getPhn());
                row.createCell(3).setCellValue(firstItem.getBill().getPatient().getPerson().getName());
                String formattedDate = sdf.format(firstItem.getBill().getCreatedAt());
                row.createCell(4).setCellValue(formattedDate);
                row.createCell(5).setCellValue(entry.getKey());

                Row detailHeaderRow = sheet.createRow(rowIndex++);
                detailHeaderRow.createCell(6).setCellValue("Service Name");
                detailHeaderRow.createCell(7).setCellValue("Hos Fee.");
                detailHeaderRow.createCell(8).setCellValue("Staff Fee.");
                detailHeaderRow.createCell(9).setCellValue("CC Fee.");
                detailHeaderRow.createCell(10).setCellValue("Net Amount");

                for (BillItem bi : billItems) {
                    Row detailRow = sheet.createRow(rowIndex++);
                    detailRow.createCell(6).setCellValue(bi.getItem().getName());
                    detailRow.createCell(7).setCellValue(bi.getHospitalFee());
                    detailRow.createCell(8).setCellValue(bi.getStaffFee());
                    detailRow.createCell(9).setCellValue(bi.getCollectingCentreFee());
                    detailRow.createCell(10).setCellValue(bi.getNetValue());
                }

                Row footerRow = sheet.createRow(rowIndex++);
                footerRow.createCell(6).setCellValue("Total");
                footerRow.createCell(7).setCellValue(calculateTotalHospitalFeeByBillItems(billItems));
                footerRow.createCell(8).setCellValue(calculateTotalStaffFeeByBillItems(billItems));
                footerRow.createCell(9).setCellValue(calculateTotalCollectionCenterFeeByBillItems(billItems));
                footerRow.createCell(10).setCellValue(calculateTotalNetValueByBillItems(billItems));
            }

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportWeeklyOPDSummaryReportToPDF() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Weekly_Summary_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);

            document.open();

            document.add(new Paragraph("Weekly Summary Report",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));

            document.add(new Paragraph("Month: " + getMonth(),
                    FontFactory.getFont(FontFactory.HELVETICA, 12)));
            document.add(new Paragraph("Report Type: Summary",
                    FontFactory.getFont(FontFactory.HELVETICA, 12)));
            document.add(new Paragraph(" "));

            for (Map.Entry<Integer, Map<String, Map<Integer, Double>>> entry : getGroupedBillItemsWeekly().entrySet()) {
                Integer key = entry.getKey();
                Map<String, Map<Integer, Double>> weeklyData = entry.getValue();

                PdfPTable table = new PdfPTable(8);
                table.setWidthPercentage(100);
                float[] columnWidths = {3f, 1f, 1f, 1f, 1f, 1f, 1f, 1.5f};
                table.setWidths(columnWidths);

                table.addCell(new PdfPCell(new Phrase(getShiftDescription(key),
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12))));

                for (int week = 1; week <= 6; week++) {
                    table.addCell(new PdfPCell(new Phrase("Week " + week,
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12))));
                }
                table.addCell(new PdfPCell(new Phrase("Total",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12))));

                for (Map.Entry<String, Map<Integer, Double>> rowEntry : weeklyData.entrySet()) {
                    String name = rowEntry.getKey();
                    Map<Integer, Double> weekValues = rowEntry.getValue();

                    table.addCell(new PdfPCell(new Phrase(name)));

                    for (int week = 1; week <= 6; week++) {
                        table.addCell(String.valueOf(weekValues.getOrDefault(week, 0.0)));
                    }

                    double total = weekValues.values().stream().mapToDouble(Double::doubleValue).sum();
                    table.addCell(String.valueOf(total));
                }

                document.add(table);
                document.add(new Paragraph(" "));
            }

            document.close();
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportWeeklyOPDSummaryReportToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Weekly_Summary_Report.xlsx");

        try (OutputStream out = response.getOutputStream(); Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Weekly Summary Report");

            int rowIndex = 0;

            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Weekly Summary Report");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 18);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            Row metaRow1 = sheet.createRow(rowIndex++);
            metaRow1.createCell(0).setCellValue("Month: " + getMonth());

            Row metaRow2 = sheet.createRow(rowIndex++);
            metaRow2.createCell(0).setCellValue("Report Type: Summary");

            rowIndex++;

            for (Map.Entry<Integer, Map<String, Map<Integer, Double>>> entry : getGroupedBillItemsWeekly().entrySet()) {
                Integer key = entry.getKey();
                Map<String, Map<Integer, Double>> weeklyData = entry.getValue();

                Row shiftRow = sheet.createRow(rowIndex++);
                Cell shiftCell = shiftRow.createCell(0);
                shiftCell.setCellValue(getShiftDescription(key));
                CellStyle shiftStyle = workbook.createCellStyle();
                Font shiftFont = workbook.createFont();
                shiftFont.setBold(true);
                shiftStyle.setFont(shiftFont);
                shiftCell.setCellStyle(shiftStyle);

                Row headerRow = sheet.createRow(rowIndex++);
                String[] headers = {"Name", "Week 1", "Week 2", "Week 3", "Week 4", "Week 5", "Week 6", "Total"};
                for (int col = 0; col < headers.length; col++) {
                    Cell cell = headerRow.createCell(col);
                    cell.setCellValue(headers[col]);
                    CellStyle headerStyle = workbook.createCellStyle();
                    headerStyle.setFont(shiftFont);
                    headerStyle.setBorderBottom(BorderStyle.THIN);
                    cell.setCellStyle(headerStyle);
                }

                for (Map.Entry<String, Map<Integer, Double>> rowEntry : weeklyData.entrySet()) {
                    Row dataRow = sheet.createRow(rowIndex++);
                    String name = rowEntry.getKey();
                    Map<Integer, Double> weekValues = rowEntry.getValue();

                    dataRow.createCell(0).setCellValue(name);

                    double total = 0.0;
                    for (int week = 1; week <= 6; week++) {
                        double value = weekValues.getOrDefault(week, 0.0);
                        dataRow.createCell(week).setCellValue(value);
                        total += value;
                    }

                    dataRow.createCell(7).setCellValue(total);
                }

                rowIndex++;
            }

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getShiftDescription(Integer key) {
        switch (key) {
            case 1:
                return "7:00 PM - 7:00 AM (Night)";
            case 2:
                return "7:00 AM - 1:00 PM";
            case 3:
                return "1:00 PM - 7:00 PM";
            default:
                return "Unknown Shift";
        }
    }

    public void exportDetailedWeeklyOPDReportToPDF() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Detailed_Weekly_OPD_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);

            document.open();

            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            com.itextpdf.text.Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            for (int week = 1; week <= 6; week++) {
                List<Integer> daysOfWeek = getDaysOfWeek(week);

                if (daysOfWeek.isEmpty()) {
                    continue;
                }

                Paragraph title = new Paragraph("Detailed Weekly OPD Report", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);

                document.add(new Paragraph("Week: " + week, headerFont));
                document.add(new Paragraph("Report Type: Detail", regularFont));
                document.add(Chunk.NEWLINE);

                addWeeklyReportSection(document, "Weekly Report OPD (7.00pm - 7.00am) Night",
                        getItemListByWeek(week, weeklyDailyBillItemMap7to7), daysOfWeek, weeklyDailyBillItemMap7to7, week, headerFont, regularFont);

                addWeeklyReportSection(document, "Weekly Report OPD (7.00pm - 1.00pm) Night",
                        getItemListByWeek(week, weeklyDailyBillItemMap7to1), daysOfWeek, weeklyDailyBillItemMap7to1, week, headerFont, regularFont);

                addWeeklyReportSection(document, "Weekly Report OPD (1.00pm - 7.00am) Night",
                        getItemListByWeek(week, weeklyDailyBillItemMap1to7), daysOfWeek, weeklyDailyBillItemMap1to7, week, headerFont, regularFont);

                document.add(Chunk.NEWLINE);
            }

            document.close();
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addWeeklyReportSection(Document document, String sectionTitle, List<String> itemList,
                                        List<Integer> daysOfWeek, Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap,
                                        int week, com.itextpdf.text.Font headerFont, com.itextpdf.text.Font regularFont) throws DocumentException {
        document.add(new com.itextpdf.text.Paragraph(sectionTitle, headerFont));
        document.add(com.itextpdf.text.Chunk.NEWLINE);

        com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(daysOfWeek.size() + 2); // +2 for Item and Total columns
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        table.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Item", headerFont)));
        for (int day : daysOfWeek) {
            table.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(String.valueOf(day), headerFont)));
        }
        table.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Total", headerFont)));

        for (String item : itemList) {
            table.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(item, regularFont)));
            for (int day : daysOfWeek) {
                double count = getCountByWeekAndDay(week, day, item, weeklyDailyBillItemMap);
                table.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(String.valueOf(count), regularFont)));
            }
            double total = getSumByWeek(week, item, weeklyDailyBillItemMap);
            table.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(String.valueOf(total), regularFont)));
        }

        document.add(table);
    }

    public void exportDetailedWeeklyOPDReportToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Detailed_Weekly_OPD_Report.xlsx");

        try (OutputStream out = response.getOutputStream(); Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Detailed Weekly OPD Report");

            int rowIndex = 0;

            for (int week = 1; week <= 6; week++) {
                List<Integer> daysOfWeek = getDaysOfWeek(week);

                if (daysOfWeek.isEmpty()) {
                    continue;
                }

                Row headerRow1 = sheet.createRow(rowIndex++);
                Cell headerCell1 = headerRow1.createCell(0);
                headerCell1.setCellValue("Report Type: Detail");
                CellStyle boldStyle = workbook.createCellStyle();
                Font boldFont = workbook.createFont();
                boldFont.setBold(true);
                boldStyle.setFont(boldFont);
                headerCell1.setCellStyle(boldStyle);

                Row headerRow2 = sheet.createRow(rowIndex++);
                headerRow2.createCell(0).setCellValue("Week: " + week);

                Row headerRow3 = sheet.createRow(rowIndex++);
                headerRow3.createCell(0).setCellValue("Weekly Report OPD (7.00pm - 7.00am) Night");

                rowIndex++;

                Row columnHeaderRow = sheet.createRow(rowIndex++);
                columnHeaderRow.createCell(0).setCellValue("Item");
                int colIndex = 1;
                for (int day : daysOfWeek) {
                    columnHeaderRow.createCell(colIndex++).setCellValue(day);
                }
                columnHeaderRow.createCell(colIndex).setCellValue("Total");

                // Data Rows for (7.00 PM - 7.00 AM)
                populateDataRows(sheet, rowIndex, getItemListByWeek(week, weeklyDailyBillItemMap7to7), daysOfWeek, weeklyDailyBillItemMap7to7, week);
                rowIndex += getItemListByWeek(week, weeklyDailyBillItemMap7to7).size();

                // Section for (7.00 PM - 1.00 PM)
                rowIndex++;
                Row sectionHeaderRow1 = sheet.createRow(rowIndex++);
                sectionHeaderRow1.createCell(0).setCellValue("Weekly Report OPD (7.00pm - 1.00pm) Night");

                populateDataRows(sheet, rowIndex, getItemListByWeek(week, weeklyDailyBillItemMap7to1), daysOfWeek, weeklyDailyBillItemMap7to1, week);
                rowIndex += getItemListByWeek(week, weeklyDailyBillItemMap7to1).size();

                // Section for (1.00 PM - 7.00 AM)
                rowIndex++;
                Row sectionHeaderRow2 = sheet.createRow(rowIndex++);
                sectionHeaderRow2.createCell(0).setCellValue("Weekly Report OPD (1.00pm - 7.00am) Night");

                populateDataRows(sheet, rowIndex, getItemListByWeek(week, weeklyDailyBillItemMap1to7), daysOfWeek, weeklyDailyBillItemMap1to7, week);
                rowIndex += getItemListByWeek(week, weeklyDailyBillItemMap1to7).size();

                rowIndex++;
            }

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populateDataRows(Sheet sheet, int rowIndex, List<String> itemList, List<Integer> daysOfWeek, Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap, int week) {
        for (String item : itemList) {
            Row dataRow = sheet.createRow(rowIndex++);
            int colIndex = 0;

            dataRow.createCell(colIndex++).setCellValue(item);

            for (int day : daysOfWeek) {
                double count = getCountByWeekAndDay(week, day, item, weeklyDailyBillItemMap);
                dataRow.createCell(colIndex++).setCellValue(count);
            }

            double total = getSumByWeek(week, item, weeklyDailyBillItemMap);
            dataRow.createCell(colIndex).setCellValue(total);
        }
    }

    public void exportRouteAnalysisDetailReportToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Collecting_Center_Monthly_Report.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Monthly Report");
            int rowIndex = 0;

            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("S. No");
            headerRow.createCell(1).setCellValue("Collecting Center Code");
            headerRow.createCell(2).setCellValue("Collecting Center");

            List<YearMonth> yearMonths = getYearMonths();
            int dynamicColumnIndex = 3;
            for (YearMonth yearMonth : yearMonths) {
                headerRow.createCell(dynamicColumnIndex++).setCellValue(yearMonth.toString() + " - Sample Count");
                headerRow.createCell(dynamicColumnIndex++).setCellValue(yearMonth.toString() + " - Service Amount");
            }

            int serialNumber = 1;
            for (Map.Entry<Institution, Map<YearMonth, Bill>> entrySet : getGroupedCollectingCenterWiseBillsMonthly().entrySet()) {
                Institution center = entrySet.getKey();
                Map<YearMonth, Bill> monthlyData = entrySet.getValue();

                Row dataRow = sheet.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(serialNumber++);
                dataRow.createCell(1).setCellValue(center.getCode());
                dataRow.createCell(2).setCellValue(center.getName());

                dynamicColumnIndex = 3;
                for (YearMonth yearMonth : yearMonths) {
                    Bill bill = monthlyData.get(yearMonth);

                    if (bill != null) {
                        dataRow.createCell(dynamicColumnIndex++).setCellValue(bill.getQty());
                        dataRow.createCell(dynamicColumnIndex++).setCellValue(bill.getTotalHospitalFee());
                    } else {
                        dataRow.createCell(dynamicColumnIndex++).setCellValue(0);
                        dataRow.createCell(dynamicColumnIndex++).setCellValue(0.0);
                    }
                }
            }

            Row totalRow = sheet.createRow(rowIndex++);
            totalRow.createCell(0).setCellValue("Total");
            totalRow.createCell(1).setCellValue("");
            totalRow.createCell(2).setCellValue("");

            dynamicColumnIndex = 3;
            for (YearMonth yearMonth : yearMonths) {
                totalRow.createCell(dynamicColumnIndex++).setCellValue(String.format("%.2f", getCollectionCenterWiseTotalSampleCount(yearMonth)
                        / calculateCollectionCenterWiseBillCount(yearMonth)));
                totalRow.createCell(dynamicColumnIndex++).setCellValue(String.format("%.2f", getCollectionCenterWiseTotalServiceAmount(yearMonth)
                        / calculateCollectionCenterWiseBillCount(yearMonth)));
            }

            workbook.write(out);
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportRouteAnalysisDetailReportToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Collecting_Center_Monthly_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Collecting Center Monthly Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(3 + getYearMonths().size() * 2);
            table.setWidthPercentage(100);

            table.addCell(new PdfPCell(new Phrase("S. No")));
            table.addCell(new PdfPCell(new Phrase("Collecting Center Code")));
            table.addCell(new PdfPCell(new Phrase("Collecting Center")));

            List<YearMonth> yearMonths = getYearMonths();
            for (YearMonth yearMonth : yearMonths) {
                table.addCell(new PdfPCell(new Phrase(yearMonth.toString() + " - Sample Count")));
                table.addCell(new PdfPCell(new Phrase(yearMonth.toString() + " - Service Amount")));
            }

            int serialNumber = 1;
            for (Map.Entry<Institution, Map<YearMonth, Bill>> entrySet : getGroupedCollectingCenterWiseBillsMonthly().entrySet()) {
                Institution center = entrySet.getKey();
                Map<YearMonth, Bill> monthlyData = entrySet.getValue();

                table.addCell(new PdfPCell(new Phrase(String.valueOf(serialNumber++))));
                table.addCell(new PdfPCell(new Phrase(center.getCode())));
                table.addCell(new PdfPCell(new Phrase(center.getName())));

                for (YearMonth yearMonth : yearMonths) {
                    Bill bill = monthlyData.get(yearMonth);
                    if (bill != null) {
                        table.addCell(new PdfPCell(new Phrase(String.valueOf(bill.getQty()))));
                        table.addCell(new PdfPCell(new Phrase(String.valueOf(bill.getTotalHospitalFee()))));
                    } else {
                        table.addCell(new PdfPCell(new Phrase("0")));
                        table.addCell(new PdfPCell(new Phrase("0.0")));
                    }
                }
            }

            PdfPCell totalCell = new PdfPCell(new Phrase("Total"));
            totalCell.setColspan(3);
            totalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(totalCell);

            for (YearMonth yearMonth : yearMonths) {
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f", getCollectionCenterWiseTotalSampleCount(yearMonth)
                        / calculateCollectionCenterWiseBillCount(yearMonth)))));
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f", getCollectionCenterWiseTotalServiceAmount(yearMonth)
                        / calculateCollectionCenterWiseBillCount(yearMonth)))));
            }

            document.add(table);
            document.close();
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportRouteAnalysisSummaryReportToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Route_Wise_Monthly_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Route Wise Monthly Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(2 + getYearMonths().size() * 2);
            table.setWidthPercentage(100);

            table.addCell(new PdfPCell(new Phrase("S. No")));
            table.addCell(new PdfPCell(new Phrase("Route")));

            List<YearMonth> yearMonths = getYearMonths();
            for (YearMonth yearMonth : yearMonths) {
                table.addCell(new PdfPCell(new Phrase(yearMonth.toString() + " - Sample Count")));
                table.addCell(new PdfPCell(new Phrase(yearMonth.toString() + " - Service Amount")));
            }

            int serialNumber = 1;
            for (Map.Entry<Route, Map<YearMonth, Bill>> entrySet : getGroupedRouteWiseBillsMonthly().entrySet()) {
                Route route = entrySet.getKey();
                Map<YearMonth, Bill> monthlyData = entrySet.getValue();

                table.addCell(new PdfPCell(new Phrase(String.valueOf(serialNumber++))));
                table.addCell(new PdfPCell(new Phrase(route.getName())));

                for (YearMonth yearMonth : yearMonths) {
                    Bill billData = monthlyData.get(yearMonth);
                    if (billData != null) {
                        table.addCell(new PdfPCell(new Phrase(String.valueOf(billData.getQty()))));
                        table.addCell(new PdfPCell(new Phrase(String.valueOf(billData.getTotalHospitalFee()))));
                    } else {
                        table.addCell(new PdfPCell(new Phrase("0")));
                        table.addCell(new PdfPCell(new Phrase("0.0")));
                    }
                }
            }

            PdfPCell totalCell = new PdfPCell(new Phrase("Total"));
            totalCell.setColspan(2);
            totalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(totalCell);

            for (YearMonth yearMonth : yearMonths) {
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f", calculateRouteWiseTotalSampleCount(yearMonth)
                        / calculateRouteWiseBillCount(yearMonth)))));
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f", calculateRouteWiseTotalServiceAmount(yearMonth)
                        / calculateRouteWiseBillCount(yearMonth)))));
            }

            document.add(table);
            document.close();
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportRouteAnalysisSummaryReportToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Route_Wise_Monthly_Report.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Route Wise Monthly Report");
            int rowIndex = 0;

            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Route Wise Monthly Report");
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2 + getYearMonths().size() * 2 - 1));

            Row headerRow = sheet.createRow(rowIndex++);
            int cellIndex = 0;
            headerRow.createCell(cellIndex++).setCellValue("S. No");
            headerRow.createCell(cellIndex++).setCellValue("Route");

            List<YearMonth> yearMonths = getYearMonths();
            for (YearMonth yearMonth : yearMonths) {
                headerRow.createCell(cellIndex++).setCellValue(yearMonth.toString() + " - Sample Count");
                headerRow.createCell(cellIndex++).setCellValue(yearMonth.toString() + " - Service Amount");
            }

            int serialNumber = 1;
            for (Map.Entry<Route, Map<YearMonth, Bill>> entrySet : getGroupedRouteWiseBillsMonthly().entrySet()) {
                Route route = entrySet.getKey();
                Map<YearMonth, Bill> monthlyData = entrySet.getValue();

                Row row = sheet.createRow(rowIndex++);
                cellIndex = 0;

                row.createCell(cellIndex++).setCellValue(serialNumber++);
                row.createCell(cellIndex++).setCellValue(route.getName());

                for (YearMonth yearMonth : yearMonths) {
                    Bill billData = monthlyData.get(yearMonth);
                    if (billData != null) {
                        row.createCell(cellIndex++).setCellValue(billData.getQty());
                        row.createCell(cellIndex++).setCellValue(billData.getTotalHospitalFee());
                    } else {
                        row.createCell(cellIndex++).setCellValue(0);
                        row.createCell(cellIndex++).setCellValue(0.0);
                    }
                }
            }

            Row totalRow = sheet.createRow(rowIndex++);
            totalRow.createCell(0).setCellValue("Total");
            totalRow.createCell(1).setCellValue("");

            cellIndex = 2;
            for (YearMonth yearMonth : yearMonths) {
                totalRow.createCell(cellIndex++).setCellValue(
                        String.format("%.2f", calculateRouteWiseTotalSampleCount(yearMonth)
                                / calculateRouteWiseBillCount(yearMonth)));
                totalRow.createCell(cellIndex++).setCellValue(
                        String.format("%.2f", calculateRouteWiseTotalServiceAmount(yearMonth)
                                / calculateRouteWiseBillCount(yearMonth)));
            }

            workbook.write(out);
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Double calculateIpGrossAmountSubTotalByBills(List<Bill> bills) {
        Double billTotal = 0.0;

        for (Bill bill : bills) {
            billTotal += bill.getPatientEncounter().getFinalBill().getGrantTotal();
        }

        return billTotal;
    }

    public Double calculateIpDiscountSubTotalByBills(List<Bill> bills) {
        Double discount = 0.0;

        for (Bill bill : bills) {
            discount += bill.getPatientEncounter().getFinalBill().getDiscount();
        }

        return discount;
    }

    public Double calculateIpNetAmountSubTotalByBills(List<Bill> bills) {
        Double netTotal = 0.0;

        for (Bill bill : bills) {
            netTotal += bill.getPatientEncounter().getFinalBill().getNetTotal();
        }

        return netTotal;
    }

    public Double calculateIpPatientShareSubTotalByBills(List<Bill> bills) {
        Double settledAmountByPatient = 0.0;

        for (Bill bill : bills) {
            settledAmountByPatient += bill.getPatientEncounter().getFinalBill().getSettledAmountByPatient();
        }

        return settledAmountByPatient;
    }

    public Double calculateIpSponsorShareSubTotalByBills(List<Bill> bills) {
        Double settledAmountBySponsor = 0.0;

        for (Bill bill : bills) {
            settledAmountBySponsor += bill.getPatientEncounter().getFinalBill().getSettledAmountBySponsor();
        }

        return settledAmountBySponsor;
    }

    public Double calculateIpDueAmountSubTotalByBills(List<Bill> bills) {
        Double balance = 0.0;

        for (Bill bill : bills) {
            balance += bill.getPatientEncounter().getFinalBill().getNetTotal() - bill.getPatientEncounter().getFinalBill().getSettledAmountBySponsor() - bill.getPatientEncounter().getFinalBill().getSettledAmountByPatient();
        }

        return balance;
    }

    public Double calculateIpGrossAmountNetTotal() {
        double grossAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            grossAmountNetTotal += calculateIpGrossAmountSubTotalByBills(bills);
        }

        return grossAmountNetTotal;
    }

    public Double calculateIpDiscountNetTotal() {
        double discountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            discountNetTotal += calculateIpDiscountSubTotalByBills(bills);
        }

        return discountNetTotal;
    }

    public Double calculateIpNetAmountNetTotal() {
        double netAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            netAmountNetTotal += calculateIpNetAmountSubTotalByBills(bills);
        }

        return netAmountNetTotal;
    }

    public Double calculateIpPatientShareNetTotal() {
        double patientShareNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            patientShareNetTotal += calculateIpPatientShareSubTotalByBills(bills);
        }

        return patientShareNetTotal;
    }

    public Double calculateIpSponsorShareNetTotal() {
        double sponsorShareNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            sponsorShareNetTotal += calculateIpSponsorShareSubTotalByBills(bills);

        }

        return sponsorShareNetTotal;
    }

    public Double calculateIpDueAmountNetTotal() {
        double dueAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            dueAmountNetTotal += calculateIpDueAmountSubTotalByBills(bills);
        }

        return dueAmountNetTotal;
    }

}
