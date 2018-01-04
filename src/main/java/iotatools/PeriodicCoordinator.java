package iotatools;

import jota.IotaAPI;
import jota.dto.response.GetNodeInfoResponse;
import jota.dto.response.GetTransactionsToApproveResponse;
import jota.model.Transaction;
import org.apache.commons.cli.*;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static iotatools.TestnetCoordinator.NULL_HASH;
import static iotatools.TestnetCoordinator.newMilestone;

@SuppressWarnings("PointlessBooleanExpression")
public class PeriodicCoordinator {

    private final static Logger logger = Logger.getLogger(PeriodicCoordinator.class.getName());

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {

        // parse arguments
        CommandLine parameter = parseArgs(args);
        String host = parameter.getOptionValue("host", "localhost");
        String port = parameter.getOptionValue("port", "14265");
        final String referenceTag = parameter.getOptionValue("tag", null);
        final Integer interval = Integer.valueOf(parameter.getOptionValue("interval", "60"));
        final Integer depth = Integer.valueOf(parameter.getOptionValue("depth", "10"));
        final IotaAPI api = new IotaAPI.Builder().host(host).port(port).build();

        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    GetNodeInfoResponse nodeInfo = api.getNodeInfo();
                    String latestSolidSubtangleMilestone = nodeInfo.getLatestSolidSubtangleMilestone();
                    int updatedMilestone = nodeInfo.getLatestMilestoneIndex() + 1;
                    if (nodeInfo.getLatestMilestone().equals(NULL_HASH)) {
                        newMilestone(api, NULL_HASH, NULL_HASH, updatedMilestone);
                    } else {
                        String trunkTransaction = latestSolidSubtangleMilestone;
                        String branchTransaction = latestSolidSubtangleMilestone;
                        String trunkReason = "default";
                        String branchReason = "default";

                        GetTransactionsToApproveResponse x = api.getTransactionsToApprove(depth);
                        if( x != null ) {
                            trunkTransaction = x.getTrunkTransaction();
                            branchTransaction = x.getBranchTransaction();
                            trunkReason = "approval";
                            branchReason = "approval";
                        }

                        // validate remaining tips
                        String[] tips = api.getTips().getHashes();
                        if( tips.length > 0 ) {
//                            List<Transaction> bundle = api.getBundle(tips[0]).getTransactions();
//                            branchTransaction = bundle.get(0).getHash();
                            branchTransaction = tips[0];
                            branchReason = "tip";
                        }

                        // find transaction with reference
                        if (referenceTag != null) {
                            String[] transactionHashes = api.findTransactions(null, new String[]{referenceTag}, null, null).getHashes();
                            boolean[] inclusionStates = api.getLatestInclusion(transactionHashes).getStates();

                            for (int i = 0; i < inclusionStates.length; i++) {
                                if (inclusionStates[i] == false) {
                                    branchTransaction = transactionHashes[i];
                                    branchReason = "tag";
                                }
                            }
                        }

                        // check if milestone is necessary
                        if ((branchTransaction.equals(trunkTransaction)) && trunkTransaction.equals(latestSolidSubtangleMilestone)) {
                            logger.info("Skipping milestone");
                        } else {
                            newMilestone(api, trunkTransaction, branchTransaction, updatedMilestone);
                            logger.info(String.format("New milestone. Approved trunk: %s (reason: %s), branch: %s (reason: %s)",
                                    trunkTransaction, trunkReason, branchTransaction, branchReason));

                            // wait for milestone to propagate
                            Thread.sleep(10000);
                        }
                    }
                }
                catch (Exception e) {
                    logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }

                executor.schedule(this, interval, TimeUnit.SECONDS);
            }
        };

        // start execution
        task.run();
    }

    private  static CommandLine parseArgs(String[] args) {
        Options options = new Options();
        options.addOption("h", "host", true, "IRI host");
        options.addOption("p", "port", true, "IRI port");
        options.addOption("i", "interval", true, "Interval (seconds) for issuing new milestones");
        options.addOption("t", "tag", true, "Reference to approve transactions with this tag");
        options.addOption("d", "depth", true, "Depth to search for tips.");

        try {
            CommandLineParser parser = new DefaultParser();
            return parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            logger.info(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }
        return null;
    }
}
