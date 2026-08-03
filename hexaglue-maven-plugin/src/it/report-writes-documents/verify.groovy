def log = new File(basedir, 'build.log').text
def output = new File(basedir, 'target/hexaglue')

assert log.contains('document(s) to') : 'the goal should say what it wrote: ' + log

def audit = new File(output, 'report/architecture-audit.md')
assert audit.exists() : 'the audit report should be written where the document asked: ' + output.list()
assert audit.text.contains('## Verdict') : 'the report should carry its sections: ' + audit.text
assert audit.text.contains('Order') : 'the report should name what was analysed: ' + audit.text

assert new File(output, 'report/architecture-audit.json').exists() : 'the data form should be written too'

def domain = new File(output, 'living-doc/domain.md')
assert domain.exists() : 'the living documentation should be written too'
assert !domain.text.contains('```mermaid') : 'a backend that was told not to draw should not draw: ' + domain.text

return true
