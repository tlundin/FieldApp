import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Standalone workflow simulator for Executor-like control flow.
 *
 * <p>Usage examples:
 * <pre>
 *   javac tools/WorkflowExecutorHarness.java
 *   java -cp tools WorkflowExecutorHarness --xml app/src/main/res/raw/rlogis.xml --list
 *   java -cp tools WorkflowExecutorHarness --xml app/src/main/res/raw/rlogis.xml --workflow Main
 *   java -cp tools WorkflowExecutorHarness --xml app/src/main/res/raw/rlogis.xml --workflow Main --var year=2026 --var role=admin
 * </pre>
 *
 * <p>Notes:
 * - This is a simulation harness intended for fast iteration outside Android.
 * - It focuses on control/mutation flow (jump, conditional continuation, gosub, set_value).
 * - UI "create" blocks are treated as deferred render blocks and only counted/reported.
 */
public final class WorkflowExecutorHarness {

    private static final String STOP_ID = "stop";
    private static final int DEFAULT_MAX_STEPS = 100_000;

    public static void main(String[] args) throws Exception {
        CliConfig cli = CliConfig.parse(args);
        if (cli.showHelp) {
            CliConfig.printHelp();
            return;
        }

        File xml = new File(cli.xmlPath);
        if (!xml.exists()) {
            System.err.println("XML file not found: " + xml.getAbsolutePath());
            System.exit(2);
            return;
        }

        Bundle bundle = BundleParser.parse(xml);
        if (bundle.workflowsByName.isEmpty()) {
            System.err.println("No workflows found in: " + xml.getAbsolutePath());
            System.exit(3);
            return;
        }

        if (cli.listOnly) {
            System.out.println("Workflows in bundle:");
            for (String wfName : bundle.workflowsByName.keySet()) {
                Workflow wf = bundle.workflowsByName.get(wfName);
                System.out.println(" - " + wf.name + " (" + wf.blocks.size() + " blocks)");
            }
            return;
        }

        String startWorkflow = cli.workflowName;
        if (startWorkflow == null || startWorkflow.isEmpty()) {
            startWorkflow = bundle.workflowsByName.keySet().iterator().next();
            System.out.println("No --workflow provided, defaulting to: " + startWorkflow);
        }

        WorkflowSimulator simulator = new WorkflowSimulator(bundle, cli.variables, cli.maxSteps);
        SimulationResult result = simulator.run(startWorkflow);
        result.print();
    }

    private static final class CliConfig {
        String xmlPath = "app/src/main/res/raw/rlogis.xml";
        String workflowName;
        boolean listOnly;
        boolean showHelp;
        int maxSteps = DEFAULT_MAX_STEPS;
        Map<String, String> variables = new LinkedHashMap<>();

        static CliConfig parse(String[] args) {
            CliConfig cfg = new CliConfig();
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                switch (a) {
                    case "--xml":
                        cfg.xmlPath = requireNext(args, ++i, "--xml");
                        break;
                    case "--workflow":
                        cfg.workflowName = requireNext(args, ++i, "--workflow");
                        break;
                    case "--var":
                        parseVar(requireNext(args, ++i, "--var"), cfg.variables);
                        break;
                    case "--list":
                        cfg.listOnly = true;
                        break;
                    case "--max-steps":
                        cfg.maxSteps = Integer.parseInt(requireNext(args, ++i, "--max-steps"));
                        break;
                    case "--help":
                    case "-h":
                        cfg.showHelp = true;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown argument: " + a);
                }
            }
            return cfg;
        }

        static void printHelp() {
            System.out.println("WorkflowExecutorHarness");
            System.out.println("  --xml <path>          Path to workflow bundle xml (default: app/src/main/res/raw/rlogis.xml)");
            System.out.println("  --list                List workflows and exit");
            System.out.println("  --workflow <name>     Start workflow name");
            System.out.println("  --var k=v             Seed variable value (repeatable)");
            System.out.println("  --max-steps <n>       Safety stop for simulation loop");
            System.out.println("  --help, -h            Show this help");
        }

        private static String requireNext(String[] args, int idx, String flag) {
            if (idx >= args.length) {
                throw new IllegalArgumentException("Missing value after " + flag);
            }
            return args[idx];
        }

        private static void parseVar(String assignment, Map<String, String> out) {
            int p = assignment.indexOf('=');
            if (p <= 0 || p == assignment.length() - 1) {
                throw new IllegalArgumentException("Invalid --var format, expected key=value but got: " + assignment);
            }
            out.put(assignment.substring(0, p).trim(), assignment.substring(p + 1).trim());
        }
    }

    private static final class Bundle {
        final Map<String, Workflow> workflowsByName = new LinkedHashMap<>();
    }

    private static final class Workflow {
        final String name;
        final List<BlockData> blocks;
        final Map<String, Integer> blockIndexById;

        Workflow(String name, List<BlockData> blocks) {
            this.name = name;
            this.blocks = blocks;
            this.blockIndexById = new HashMap<>();
            for (int i = 0; i < blocks.size(); i++) {
                String id = blocks.get(i).blockId;
                if (id != null) {
                    blockIndexById.put(id, i);
                }
            }
        }
    }

    private static final class BlockData {
        final String tag;
        final String blockId;
        final Map<String, String> fields;

        BlockData(String tag, String blockId, Map<String, String> fields) {
            this.tag = tag;
            this.blockId = blockId;
            this.fields = fields;
        }

        String get(String key) {
            return fields.get(key);
        }
    }

    private static final class BundleParser {
        static Bundle parse(File xmlFile) throws Exception {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            if (!"bundle".equals(root.getTagName())) {
                throw new IllegalArgumentException("Expected <bundle> root element, got <" + root.getTagName() + ">");
            }

            Bundle bundle = new Bundle();
            NodeList workflowNodes = root.getElementsByTagName("workflow");
            int unnamedCounter = 1;
            for (int i = 0; i < workflowNodes.getLength(); i++) {
                Node n = workflowNodes.item(i);
                if (n.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element wfEl = (Element) n;
                Element blocksEl = firstChildElementByTag(wfEl, "blocks");
                if (blocksEl == null) {
                    continue;
                }
                List<BlockData> blocks = parseBlocks(blocksEl);
                String wfName = inferWorkflowName(blocks);
                if (wfName == null || wfName.isEmpty()) {
                    wfName = "_unnamed_" + unnamedCounter++;
                }
                if (bundle.workflowsByName.containsKey(wfName)) {
                    wfName = wfName + "#" + unnamedCounter++;
                }
                bundle.workflowsByName.put(wfName, new Workflow(wfName, blocks));
            }
            return bundle;
        }

        private static List<BlockData> parseBlocks(Element blocksEl) {
            NodeList children = blocksEl.getChildNodes();
            List<BlockData> blocks = new ArrayList<>();
            for (int i = 0; i < children.getLength(); i++) {
                Node n = children.item(i);
                if (n.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element blockEl = (Element) n;
                String tag = blockEl.getTagName();
                Map<String, String> fields = extractDirectTextChildren(blockEl);
                String blockId = fields.get("block_ID");
                blocks.add(new BlockData(tag, blockId, fields));
            }
            return blocks;
        }

        private static String inferWorkflowName(List<BlockData> blocks) {
            for (BlockData b : blocks) {
                if ("block_start".equals(b.tag)) {
                    String name = b.get("workflowname");
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
            for (BlockData b : blocks) {
                if ("block_define_page".equals(b.tag)) {
                    String label = b.get("label");
                    if (label != null && !label.trim().isEmpty()) {
                        return label.trim();
                    }
                }
            }
            return null;
        }

        private static Map<String, String> extractDirectTextChildren(Element parent) {
            Map<String, String> map = new LinkedHashMap<>();
            NodeList children = parent.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node n = children.item(i);
                if (n.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element child = (Element) n;
                String key = child.getTagName();
                String value = child.getTextContent();
                if (value != null) {
                    value = value.trim();
                }
                map.put(key, value);
            }
            return map;
        }

        private static Element firstChildElementByTag(Element parent, String tag) {
            NodeList children = parent.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node n = children.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) n;
                    if (tag.equals(e.getTagName())) {
                        return e;
                    }
                }
            }
            return null;
        }
    }

    private static final class WorkflowSimulator {
        private final Bundle bundle;
        private final Map<String, String> variables = new LinkedHashMap<>();
        private final int maxSteps;

        WorkflowSimulator(Bundle bundle, Map<String, String> seedVariables, int maxSteps) {
            this.bundle = bundle;
            this.variables.putAll(seedVariables);
            this.maxSteps = maxSteps;
        }

        SimulationResult run(String startWorkflowName) {
            Workflow wf = bundle.workflowsByName.get(startWorkflowName);
            if (wf == null) {
                return SimulationResult.error("Workflow not found: " + startWorkflowName, bundle.workflowsByName.keySet());
            }

            Deque<Frame> stack = new ArrayDeque<>();
            Frame frame = new Frame(wf, 0);
            int steps = 0;
            List<String> trace = new ArrayList<>();
            List<UiComponent> deferredUi = new ArrayList<>();
            List<RerunTrigger> rerunTriggers = new ArrayList<>();
            int mutationCount = 0;

            while (frame != null) {
                if (++steps > maxSteps) {
                    return SimulationResult.maxSteps(trace, deferredUi, variables, maxSteps);
                }

                if (frame.pointer >= frame.workflow.blocks.size()) {
                    frame = stack.pollLast();
                    continue;
                }

                BlockData b = frame.workflow.blocks.get(frame.pointer);
                String blockId = b.blockId != null ? b.blockId : ("@" + frame.pointer);
                trace.add(frame.workflow.name + ":" + frame.pointer + ":" + b.tag + "[" + blockId + "]");

                String jumpTarget = null;
                switch (b.tag) {
                    case "block_jump":
                        jumpTarget = b.get("next_block_ID");
                        break;

                    case "block_conditional_continuation": {
                        String expr = nullToEmpty(b.get("expression"));
                        boolean cond = Eval.evalBoolean(expr, variables);
                        rerunTriggers.add(RerunTrigger.forConditional(frame.workflow.name, blockId, expr,
                                "Potential re-run on onSave when conditional evaluation changes"));
                        if (cond) {
                            jumpTarget = null; // next
                        } else {
                            jumpTarget = b.get("else_block_ID");
                        }
                        break;
                    }

                    case "block_set_value": {
                        String target = b.get("target");
                        String expression = b.get("expression");
                        if (target != null && expression != null) {
                            String previous = variables.get(target);
                            String evaluated = Eval.evalValue(expression, variables);
                            variables.put(target, evaluated);
                            mutationCount++;
                            String behavior = nullToEmpty(b.get("execution_behavior"));
                            if ("update_flow".equalsIgnoreCase(behavior) && !Objects.equals(previous, evaluated)) {
                                rerunTriggers.add(RerunTrigger.forSetValue(frame.workflow.name, blockId, target, previous, evaluated,
                                        "execution_behavior=update_flow and value changed"));
                            }
                        }
                        break;
                    }

                    case "block_go_sub": {
                        String targetWf = b.get("target");
                        Workflow sub = targetWf == null ? null : bundle.workflowsByName.get(targetWf);
                        if (sub != null) {
                            stack.addLast(new Frame(frame.workflow, frame.pointer + 1));
                            frame = new Frame(sub, 0);
                            continue;
                        }
                        break;
                    }

                    default:
                        if (isLikelyUiCreateBlock(b.tag)) {
                            deferredUi.add(UiComponent.from(frame.workflow.name, blockId, b));
                        } else if (isLikelyMutationBlock(b.tag)) {
                            mutationCount++;
                        }
                        break;
                }

                if (STOP_ID.equals(jumpTarget)) {
                    frame = stack.pollLast();
                    continue;
                }

                if (jumpTarget != null) {
                    Integer idx = frame.workflow.blockIndexById.get(jumpTarget);
                    if (idx == null) {
                        trace.add(frame.workflow.name + ":!invalid_jump_target:" + jumpTarget);
                        frame = stack.pollLast();
                    } else {
                        frame.pointer = idx;
                    }
                } else {
                    frame.pointer++;
                }
            }

            return SimulationResult.ok(trace, deferredUi, rerunTriggers, variables, mutationCount);
        }

        private static boolean isLikelyUiCreateBlock(String tag) {
            if (tag == null) {
                return false;
            }
            return tag.startsWith("block_create_")
                    || "block_button".equals(tag)
                    || "block_define_menu_entry".equals(tag)
                    || "block_define_menu_header".equals(tag)
                    || "block_add_rule".equals(tag);
        }

        private static boolean isLikelyMutationBlock(String tag) {
            return Objects.equals("block_add_filter", tag)
                    || Objects.equals("block_add_variable_to_entry_field", tag)
                    || Objects.equals("block_add_variable_to_every_list_entry", tag)
                    || Objects.equals("block_add_variable_to_list_entry", tag)
                    || Objects.equals("block_delete_matching_variables", tag);
        }
    }

    private static final class Frame {
        final Workflow workflow;
        int pointer;

        Frame(Workflow workflow, int pointer) {
            this.workflow = workflow;
            this.pointer = pointer;
        }
    }

    private static final class SimulationResult {
        final boolean ok;
        final String message;
        final List<String> knownWorkflows;
        final List<String> trace;
        final List<UiComponent> deferredUi;
        final List<RerunTrigger> rerunTriggers;
        final Map<String, String> finalVariables;
        final int mutationCount;

        private SimulationResult(boolean ok,
                                 String message,
                                 List<String> knownWorkflows,
                                 List<String> trace,
                                 List<UiComponent> deferredUi,
                                 List<RerunTrigger> rerunTriggers,
                                 Map<String, String> finalVariables,
                                 int mutationCount) {
            this.ok = ok;
            this.message = message;
            this.knownWorkflows = knownWorkflows;
            this.trace = trace;
            this.deferredUi = deferredUi;
            this.rerunTriggers = rerunTriggers;
            this.finalVariables = finalVariables;
            this.mutationCount = mutationCount;
        }

        static SimulationResult ok(List<String> trace,
                                   List<UiComponent> deferredUi,
                                   List<RerunTrigger> rerunTriggers,
                                   Map<String, String> finalVariables,
                                   int mutationCount) {
            return new SimulationResult(true, null, Collections.emptyList(),
                    trace, deferredUi, rerunTriggers, new LinkedHashMap<>(finalVariables), mutationCount);
        }

        static SimulationResult error(String message, Iterable<String> known) {
            List<String> names = new ArrayList<>();
            for (String k : known) {
                names.add(k);
            }
            return new SimulationResult(false, message, names,
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), 0);
        }

        static SimulationResult maxSteps(List<String> trace,
                                         List<UiComponent> deferredUi,
                                         Map<String, String> finalVariables,
                                         int maxSteps) {
            return new SimulationResult(false,
                    "Aborted after reaching max steps: " + maxSteps,
                    Collections.emptyList(),
                    trace,
                    deferredUi,
                    Collections.emptyList(),
                    new LinkedHashMap<>(finalVariables),
                    0);
        }

        void print() {
            if (!ok) {
                System.out.println("Simulation status: FAILED");
                System.out.println("Reason: " + message);
                if (!knownWorkflows.isEmpty()) {
                    System.out.println("Known workflows:");
                    for (String wf : knownWorkflows) {
                        System.out.println(" - " + wf);
                    }
                }
            } else {
                System.out.println("Simulation status: OK");
            }

            System.out.println();
            System.out.println("Executed block count: " + trace.size());
            System.out.println("Deferred UI block count: " + deferredUi.size());
            System.out.println("Potential re-run trigger count: " + rerunTriggers.size());
            System.out.println("Mutation count: " + mutationCount);

            System.out.println();
            System.out.println("Final UI components:");
            if (deferredUi.isEmpty()) {
                System.out.println(" (none)");
            } else {
                for (int i = 0; i < deferredUi.size() && i < 200; i++) {
                    System.out.println(" " + deferredUi.get(i).toLine());
                }
                if (deferredUi.size() > 200) {
                    System.out.println(" ... (" + (deferredUi.size() - 200) + " more)");
                }
            }

            System.out.println();
            System.out.println("Re-run trigger points:");
            if (rerunTriggers.isEmpty()) {
                System.out.println(" (none)");
            } else {
                for (RerunTrigger t : rerunTriggers) {
                    System.out.println(" " + t.toLine());
                }
            }

            System.out.println();
            System.out.println("Final variable values:");
            if (finalVariables.isEmpty()) {
                System.out.println(" (none)");
            } else {
                for (Map.Entry<String, String> e : finalVariables.entrySet()) {
                    System.out.println(" " + e.getKey() + " = " + e.getValue());
                }
            }

            System.out.println();
            System.out.println("Execution trace (first 120 lines):");
            for (int i = 0; i < trace.size() && i < 120; i++) {
                System.out.println(" " + trace.get(i));
            }
            if (trace.size() > 120) {
                System.out.println(" ... (" + (trace.size() - 120) + " more)");
            }
        }
    }

    private static final class UiComponent {
        final String workflow;
        final String blockId;
        final String tag;
        final String name;
        final String label;
        final String container;
        final String type;
        final String target;

        private UiComponent(String workflow,
                            String blockId,
                            String tag,
                            String name,
                            String label,
                            String container,
                            String type,
                            String target) {
            this.workflow = workflow;
            this.blockId = blockId;
            this.tag = tag;
            this.name = name;
            this.label = nameOrNull(label);
            this.container = nameOrNull(container);
            this.type = nameOrNull(type);
            this.target = nameOrNull(target);
        }

        static UiComponent from(String workflow, String blockId, BlockData b) {
            return new UiComponent(
                    workflow,
                    blockId,
                    b.tag,
                    b.get("name"),
                    b.get("label"),
                    firstNonEmpty(b.get("container_name"), b.get("container")),
                    b.get("type"),
                    b.get("target")
            );
        }

        String toLine() {
            StringBuilder sb = new StringBuilder();
            sb.append(workflow).append(":").append(blockId).append(":").append(tag);
            if (name != null && !name.isEmpty()) {
                sb.append(" name=").append(name);
            }
            if (label != null && !label.isEmpty()) {
                sb.append(" label=").append(label);
            }
            if (container != null && !container.isEmpty()) {
                sb.append(" container=").append(container);
            }
            if (type != null && !type.isEmpty()) {
                sb.append(" type=").append(type);
            }
            if (target != null && !target.isEmpty()) {
                sb.append(" target=").append(target);
            }
            return sb.toString();
        }
    }

    private static final class RerunTrigger {
        final String workflow;
        final String blockId;
        final String tag;
        final String detail;
        final String reason;

        private RerunTrigger(String workflow, String blockId, String tag, String detail, String reason) {
            this.workflow = workflow;
            this.blockId = blockId;
            this.tag = tag;
            this.detail = detail;
            this.reason = reason;
        }

        static RerunTrigger forConditional(String workflow, String blockId, String expression, String reason) {
            return new RerunTrigger(workflow, blockId, "block_conditional_continuation",
                    "expression=" + nullToEmpty(expression), reason);
        }

        static RerunTrigger forSetValue(String workflow, String blockId, String target, String before, String after, String reason) {
            return new RerunTrigger(workflow, blockId, "block_set_value",
                    "target=" + nullToEmpty(target) + " before=" + nullToEmpty(before) + " after=" + nullToEmpty(after),
                    reason);
        }

        String toLine() {
            return workflow + ":" + blockId + ":" + tag + " -> " + detail + " (" + reason + ")";
        }
    }

    private static final class Eval {
        private static final DateTimeFormatter SWE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
        private static final DateTimeFormatter HH = DateTimeFormatter.ofPattern("HH");
        private static final DateTimeFormatter MM = DateTimeFormatter.ofPattern("mm");
        private static final DateTimeFormatter SS = DateTimeFormatter.ofPattern("ss");

        static boolean evalBoolean(String expr, Map<String, String> vars) {
            if (expr == null) {
                return false;
            }
            String normalized = expr.trim();
            if (normalized.isEmpty()) {
                return false;
            }
            if ("true".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("false".equalsIgnoreCase(normalized)) {
                return false;
            }

            String lower = normalized.toLowerCase(Locale.ROOT);
            if (lower.contains(" and ")) {
                String[] parts = splitOnWord(normalized, "and");
                for (String p : parts) {
                    if (!evalBoolean(p, vars)) {
                        return false;
                    }
                }
                return true;
            }
            if (lower.contains(" or ")) {
                String[] parts = splitOnWord(normalized, "or");
                for (String p : parts) {
                    if (evalBoolean(p, vars)) {
                        return true;
                    }
                }
                return false;
            }

            return evalComparison(normalized, vars);
        }

        static String evalValue(String expression, Map<String, String> vars) {
            if (expression == null) {
                return null;
            }
            String e = expression.trim();
            if (e.isEmpty()) {
                return "";
            }
            if (isQuoted(e)) {
                return unquote(e);
            }

            if (e.contains("[") && e.contains("]")) {
                return evaluateTemplate(e, vars);
            }

            if (e.contains("+")) {
                String[] parts = e.split("\\+");
                StringBuilder sb = new StringBuilder();
                for (String p : parts) {
                    sb.append(resolveToken(p.trim(), vars));
                }
                return sb.toString();
            }
            return resolveToken(e, vars);
        }

        private static boolean evalComparison(String expr, Map<String, String> vars) {
            String[] ops = {"==", "!=", ">=", "<=", ">", "<"};
            for (String op : ops) {
                int p = expr.indexOf(op);
                if (p > 0) {
                    String left = resolveToken(expr.substring(0, p).trim(), vars);
                    String right = resolveToken(expr.substring(p + op.length()).trim(), vars);
                    return compare(left, right, op);
                }
            }
            String v = resolveToken(expr, vars);
            return v != null && !v.isEmpty() && !"0".equals(v) && !"false".equalsIgnoreCase(v);
        }

        private static boolean compare(String left, String right, String op) {
            Double ln = toNumber(left);
            Double rn = toNumber(right);
            if (ln != null && rn != null) {
                switch (op) {
                    case "==":
                        return Double.compare(ln, rn) == 0;
                    case "!=":
                        return Double.compare(ln, rn) != 0;
                    case ">=":
                        return ln >= rn;
                    case "<=":
                        return ln <= rn;
                    case ">":
                        return ln > rn;
                    case "<":
                        return ln < rn;
                    default:
                        return false;
                }
            }

            int cmp = nullToEmpty(left).compareTo(nullToEmpty(right));
            switch (op) {
                case "==":
                    return Objects.equals(left, right);
                case "!=":
                    return !Objects.equals(left, right);
                case ">=":
                    return cmp >= 0;
                case "<=":
                    return cmp <= 0;
                case ">":
                    return cmp > 0;
                case "<":
                    return cmp < 0;
                default:
                    return false;
            }
        }

        private static String resolveToken(String token, Map<String, String> vars) {
            if (token == null) {
                return null;
            }
            String t = token.trim();
            if (t.isEmpty()) {
                return "";
            }
            if (isQuoted(t)) {
                return unquote(t);
            }
            if (t.contains("[") && t.contains("]")) {
                return evaluateTemplate(t, vars);
            }
            if ("null".equalsIgnoreCase(t)) {
                return null;
            }
            if ("true".equalsIgnoreCase(t) || "false".equalsIgnoreCase(t)) {
                return t.toLowerCase(Locale.ROOT);
            }
            if (isNumeric(t)) {
                return t;
            }
            if (t.startsWith("@")) {
                return vars.get(t.substring(1));
            }
            if (t.startsWith("$")) {
                return vars.getOrDefault(t.substring(1), t);
            }
            return vars.getOrDefault(t, t);
        }

        private static String evaluateTemplate(String raw, Map<String, String> vars) {
            StringBuilder out = new StringBuilder();
            int i = 0;
            while (i < raw.length()) {
                char c = raw.charAt(i);
                if (c == '[') {
                    int end = raw.indexOf(']', i + 1);
                    if (end == -1) {
                        out.append(raw.substring(i));
                        break;
                    }
                    String inner = raw.substring(i + 1, end).trim();
                    out.append(nullToEmpty(evaluateFragment(inner, vars)));
                    i = end + 1;
                } else {
                    out.append(c);
                    i++;
                }
            }
            return out.toString();
        }

        private static String evaluateFragment(String fragment, Map<String, String> vars) {
            if (fragment == null) {
                return null;
            }
            String f = fragment.trim();
            if (f.isEmpty()) {
                return "";
            }
            if (f.startsWith("{") && f.endsWith("}") && f.length() >= 2) {
                return f.substring(1, f.length() - 1);
            }
            if (isQuoted(f)) {
                return unquote(f);
            }
            if (f.startsWith("$")) {
                return vars.getOrDefault(f.substring(1), "");
            }
            if (f.startsWith("getCurrentYear()")) {
                return String.valueOf(LocalDate.now().getYear());
            }
            if (f.startsWith("getCurrentHour()")) {
                return HH.format(LocalDateTime.now());
            }
            if (f.startsWith("getCurrentMinute()")) {
                return MM.format(LocalDateTime.now());
            }
            if (f.startsWith("getCurrentSecond()")) {
                return SS.format(LocalDateTime.now());
            }
            if (f.startsWith("getSweDate()")) {
                return SWE_DATE.format(LocalDate.now());
            }
            if (f.startsWith("getColumnValue(") && f.endsWith(")")) {
                String arg = f.substring("getColumnValue(".length(), f.length() - 1).trim();
                String key = stripQuotes(arg);
                return vars.getOrDefault(key, "");
            }
            if (f.startsWith("historical(") && f.endsWith(")")) {
                String arg = f.substring("historical(".length(), f.length() - 1).trim();
                String key = stripQuotes(arg);
                return vars.getOrDefault(key, "");
            }
            // Fallback: treat fragment as token/var.
            return resolveToken(f, vars);
        }

        private static String stripQuotes(String s) {
            if (s == null) {
                return null;
            }
            String t = s.trim();
            if (isQuoted(t)) {
                return unquote(t);
            }
            return t;
        }

        private static String[] splitOnWord(String text, String word) {
            return text.split("(?i)\\s+" + word + "\\s+");
        }

        private static boolean isQuoted(String s) {
            return (s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"));
        }

        private static String unquote(String s) {
            if (s.length() >= 2 && isQuoted(s)) {
                return s.substring(1, s.length() - 1);
            }
            return s;
        }

        private static Double toNumber(String s) {
            if (!isNumeric(s)) {
                return null;
            }
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private static boolean isNumeric(String s) {
            if (s == null) {
                return false;
            }
            String t = s.trim();
            if (t.isEmpty()) {
                return false;
            }
            try {
                Double.parseDouble(t);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v;
            }
        }
        return null;
    }

    private static String nameOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
