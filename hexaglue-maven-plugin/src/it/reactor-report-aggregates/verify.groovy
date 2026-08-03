def log = new File(basedir, 'build.log').text
def output = new File(basedir, 'target/hexaglue')

assert log.contains('module(s) of the reactor as one') : 'the goal should say it read the reactor in one pass: ' + log
assert log.count('module(s) of the reactor as one') == 1 : 'the reactor should be read once, not once per module: ' + log

def audit = new File(output, 'report/architecture-audit.md')
assert audit.exists() : 'one report should be written for the whole reactor: ' + output.list()

assert !new File(basedir, 'shop-domain/target/hexaglue').exists() : 'the aggregated goal should write once, not per module'
assert !new File(basedir, 'shop-infra/target/hexaglue').exists() : 'the aggregated goal should write once, not per module'

assert audit.text.contains('Order') : 'the report should name what the domain module holds: ' + audit.text
assert audit.text.contains('InMemoryOrders') : 'the report should name what the infrastructure module holds: ' + audit.text

// The layout is read from the references, not from the module names or from what the POM declares.
assert audit.text.contains('2 module(s)') : 'the report should lay out the reactor: ' + audit.text
assert audit.text.contains('| `shop-domain` | DOMAIN | nothing |') : 'the domain module should depend on nothing: ' + audit.text
assert audit.text.contains('| `shop-infra` | INFRASTRUCTURE | shop-domain |') : 'the infrastructure module should depend on the domain: ' + audit.text

// A port declared in one module and implemented in another is one seam, and the reading of the
// whole reactor is what makes it visible: read module by module, nothing would implement Orders.
assert !audit.text.contains('HG-HEX-002') : 'nothing should report the port as unimplemented: ' + audit.text

return true
