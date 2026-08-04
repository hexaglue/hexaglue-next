def log = new File(basedir, 'build.log').text
def audit = new File(basedir, 'target/hexaglue/audit/architecture-audit.md')

assert audit.exists() : 'the audit report should be written'

// The store this project keeps its orders behind is implemented by nobody in these sources, and
// that is not a fault: the jpa backend states it writes what fills it.
assert !audit.text.contains('HG-HEX-002') : 'a port this build fills should not be reported as unfilled: ' + audit.text

// Five false alarms are not traded for an unexplained silence: the run says what it left out.
assert log.contains('HG-ENGINE-005') : 'the run should say which ports it left unreported: ' + log
assert log.contains('com.example.shop.Orders') : 'and name them: ' + log
assert log.contains('io.hexaglue.jpa') : 'and say on whose word: ' + log

return true
