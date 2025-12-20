import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.davidbohl.dirigent.utility.process.ProcessResult;
import org.davidbohl.dirigent.utility.process.ProcessRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProcessRunnerTest {

    @Test
    void testProcessRunnerWithEnv() throws IOException {

        ProcessRunner runner = new ProcessRunner();

        ProcessResult executeCommand = runner.executeCommand(List.of("printenv"), Map.of("MY_ENV", "TEST"));

        Assertions.assertEquals(0, executeCommand.exitCode());
        

    }

}
