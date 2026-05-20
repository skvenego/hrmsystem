package com.hrm.hrmsystem.component;

import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.PayrollRepository;
import com.hrm.hrmsystem.repository.PayslipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DatabaseCleanupRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCleanupRunner.class);

    private final EmployeeRepository employeeRepository;
    private final PayrollRepository payrollRepository;
    private final PayslipRepository payslipRepository;

    public DatabaseCleanupRunner(EmployeeRepository employeeRepository,
                                 PayrollRepository payrollRepository,
                                 PayslipRepository payslipRepository) {
        this.employeeRepository = employeeRepository;
        this.payrollRepository = payrollRepository;
        this.payslipRepository = payslipRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Running startup database cleanup to remove system user payroll and payslips...");
        try {
            List<Employee> employees = employeeRepository.findAll();
            int deletedPayrollCount = 0;
            int deletedPayslipCount = 0;

            for (Employee emp : employees) {
                if (isSystemUser(emp)) {
                    // Delete payrolls
                    var payrolls = payrollRepository.findByEmployeeId(emp.getId());
                    if (!payrolls.isEmpty()) {
                        payrollRepository.deleteAll(payrolls);
                        deletedPayrollCount += payrolls.size();
                    }

                    // Delete payslips
                    var payslips = payslipRepository.findByEmployeeIdOrderByMonthYearDesc(emp.getId());
                    if (!payslips.isEmpty()) {
                        payslipRepository.deleteAll(payslips);
                        deletedPayslipCount += payslips.size();
                    }
                }
            }
            if (deletedPayrollCount > 0 || deletedPayslipCount > 0) {
                log.info("Cleanup completed successfully: deleted {} payroll records and {} payslip records for system users.",
                        deletedPayrollCount, deletedPayslipCount);
            } else {
                log.info("No system user payroll or payslip records found to clean up.");
            }
        } catch (Exception e) {
            log.error("Error running database cleanup runner", e);
        }
    }

    private boolean isSystemUser(Employee emp) {
        if (emp.getDepartment() == null || emp.getDepartment().getName() == null) return false;
        String dept = emp.getDepartment().getName().toLowerCase();
        return dept.contains("hr") || dept.contains("director") ||
               dept.contains("leave") || dept.contains("accountant");
    }
}
