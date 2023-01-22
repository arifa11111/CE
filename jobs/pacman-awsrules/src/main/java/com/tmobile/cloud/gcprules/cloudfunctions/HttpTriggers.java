package com.tmobile.cloud.gcprules.cloudfunctions;

import com.amazonaws.util.StringUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tmobile.cloud.awsrules.utils.PacmanUtils;
import com.tmobile.cloud.constants.PacmanRuleConstants;
import com.tmobile.cloud.gcprules.utils.GCPUtils;
import com.tmobile.pacman.commons.PacmanSdkConstants;
import com.tmobile.pacman.commons.exception.InvalidInputException;
import com.tmobile.pacman.commons.exception.RuleExecutionFailedExeption;
import com.tmobile.pacman.commons.policy.Annotation;
import com.tmobile.pacman.commons.policy.BasePolicy;
import com.tmobile.pacman.commons.policy.PacmanPolicy;
import com.tmobile.pacman.commons.policy.PolicyResult;
import com.tmobile.pacman.commons.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.*;

@PacmanPolicy(key = "Enable-Https-For-Cloudfunc", desc = "Enable https for Cloud Functions", severity = PacmanSdkConstants.SEV_MEDIUM, category = PacmanSdkConstants.SECURITY)
public class HttpTriggers extends BasePolicy {
    private static final Logger logger = LoggerFactory.getLogger(HttpTriggers.class);

    @Override
    public PolicyResult execute(Map<String, String> ruleParam, Map<String, String> resourceAttributes) {
        logger.debug("Executing https trigger rule for gcp cloud function");
        Annotation annotation = null;
        String resourceId = ruleParam.get(PacmanRuleConstants.RESOURCE_ID);
        String severity = ruleParam.get(PacmanRuleConstants.SEVERITY);
        String category = ruleParam.get(PacmanRuleConstants.CATEGORY);
        String esUrl = CommonUtils.getEnvVariableValue(PacmanSdkConstants.ES_URI_ENV_VAR_NAME);
        if (Boolean.FALSE.equals(PacmanUtils.doesAllHaveValue(severity, category, esUrl))) {
            logger.info(PacmanRuleConstants.MISSING_CONFIGURATION);
            throw new InvalidInputException(PacmanRuleConstants.MISSING_CONFIGURATION);
        }

        if (!StringUtils.isNullOrEmpty(esUrl)) {
            esUrl = esUrl + "/gcp_cloudfunctiongen1/_search";
        }
        logger.debug("========gcp_cloudfunctiongen1 URL after concatenation param {}  =========", esUrl);
        boolean isHTTPSEnabled = false;

        MDC.put(PacmanSdkConstants.EXECUTION_ID, ruleParam.get(PacmanSdkConstants.EXECUTION_ID));
        MDC.put(PacmanSdkConstants.POLICY_ID, ruleParam.get(PacmanSdkConstants.POLICY_ID));

        if (!StringUtils.isNullOrEmpty(resourceId)) {
            logger.debug("========after url");
            Map<String, Object> mustFilter = new HashMap<>();
            mustFilter.put(PacmanUtils.convertAttributetoKeyword(PacmanRuleConstants.RESOURCE_ID), resourceId);
            mustFilter.put(PacmanRuleConstants.LATEST, true);

            try {
                isHTTPSEnabled = checkHTTPSEnabled(esUrl, mustFilter);
                if (!isHTTPSEnabled) {
                    List<LinkedHashMap<String, Object>> issueList = new ArrayList<>();
                    LinkedHashMap<String, Object> issue = new LinkedHashMap<>();

                    annotation = Annotation.buildAnnotation(ruleParam, Annotation.Type.ISSUE);
                    annotation.put(PacmanSdkConstants.DESCRIPTION, "Google Cloud Platform (GCP) cloud HTTP functions to be triggered only with HTTPS, user requests will be redirected to use the HTTPS protocol, which is more secure. It is recommended to set the 'Require HTTPS' for configuring HTTP triggers while deploying your function.");
                    annotation.put(PacmanRuleConstants.SEVERITY, severity);
                    annotation.put(PacmanRuleConstants.CATEGORY, category);
                    issue.put(PacmanRuleConstants.VIOLATION_REASON, "GCP Cloud Functions HTTP trigger is not secured");
                    issueList.add(issue);
                    annotation.put(PacmanRuleConstants.ISSUE_DETAILS, issueList.toString());
                    logger.debug("========rule ended with status failure {}", annotation);
                    return new PolicyResult(PacmanSdkConstants.STATUS_FAILURE, PacmanRuleConstants.FAILURE_MESSAGE,
                            annotation);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                throw new RuleExecutionFailedExeption(exception.getMessage());
            }
        }

        logger.debug("======== ended with status true=========");
        return new PolicyResult(PacmanSdkConstants.STATUS_SUCCESS, PacmanRuleConstants.SUCCESS_MESSAGE);

    }

    private boolean checkHTTPSEnabled(String vmEsURL, Map<String, Object> mustFilter) throws Exception {
        logger.debug("========checkHTTPSEnabled  started=========");
        boolean validationResult = true;
        try{
            JsonArray hitsJsonArray = GCPUtils.getHitsArrayFromEs(vmEsURL, mustFilter);
            if (hitsJsonArray != null && hitsJsonArray.size() > 0) {
                logger.debug("========checkHTTPSEnabled hit array=========");
                JsonObject sourceData = (JsonObject) ((JsonObject) hitsJsonArray.get(0))
                        .get(PacmanRuleConstants.SOURCE);
                logger.debug("Validating the data item: {}", sourceData);
                Integer httpTrigger = sourceData.getAsJsonObject().get(PacmanRuleConstants.HTTP_TRIGGER).getAsInt();
                /*refrence https://cloud.google.com/java/docs/reference/google-cloud-functions/latest/com.google.cloud.functions.v1.HttpsTrigger.SecurityLevel*/
                if (httpTrigger != PacmanRuleConstants.SECURE_ALWAYS) {
                    validationResult = false;
                }
            }
        }catch(Exception e){
            logger.error("Error occurred in checkHTTPSEnabled ::"+e);
            validationResult = false;
        }
        return validationResult;
    }

    @Override
    public String getHelpText() {
        return "GCP Cloud Function HTTP trigger is not secured";
    }
}
