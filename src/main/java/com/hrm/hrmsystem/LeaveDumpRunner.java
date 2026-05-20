
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.repository.LeaveRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class LeaveDumpRunner implements CommandLineRunner {
    private final LeaveRepository repository;
    public LeaveDumpRunner(LeaveRepository repository) { this.repository = repository; }
    @Override
    public void run(String... args) {
        System.out.println("--- LEAVE DUMP START ---");
        repository.findAll().forEach(l -> {
            if (l.getEmployee() != null && (l.getEmployee().getFirstName().contains("Anubhav") || l.getEmployee().getLastName().contains("Anubhav"))) {
                System.out.println("ID: " + l.getId());
                System.out.println("Type: " + l.getLeaveType());
                System.out.println("Dates: " + l.getStartDate() + " to " + l.getEndDate());
                System.out.println("Total: " + l.getTotalDays());
                System.out.println("Paid: " + l.getPaidDays());
                System.out.println("Unpaid: " + l.getUnpaidDays());
                System.out.println("FinalPaid: " + l.getFinalPaidDays());
                System.out.println("FinalUnpaid: " + l.getFinalUnpaidDays());
                System.out.println("Status: " + l.getStatus());
                System.out.println("---");
            }
        });
        System.out.println("--- LEAVE DUMP END ---");
    }
}
