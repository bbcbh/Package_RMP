package infection;

import java.util.regex.Matcher;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.solvers.BaseAbstractUnivariateSolver;
import org.apache.commons.math3.analysis.solvers.NewtonRaphsonSolver;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.special.Gamma;
import person.AbstractIndividualInterface;
import random.RandomGenerator;

/**
 *
 * @author Ben Hui
 */
public class COVID19_Remote_Infection extends AbstractInfectionWithPatientMapping {

    /**
	 * 
	 */
	private static final long serialVersionUID = 6075074964797168948L;
	public static final String[] INFECTION_STATE = new String[]{"Infected", "Immume"};
    public static final int STATUS_INFECTED = 0;
    public static final int STATUS_IMMUNED = STATUS_INFECTED + 1;

    public static final int PARAM_R0_INFECTED = 0;
    public static final int PARAM_AGE_OF_EXPOSURE = PARAM_R0_INFECTED + 1;
    public static final int PARAM_INFECTED_UNTIL_AGE = PARAM_AGE_OF_EXPOSURE + 1;
    public static final int PARAM_INFECTIOUS_START_AGE = PARAM_INFECTED_UNTIL_AGE + 1;
    public static final int PARAM_INFECTIOUS_END_AGE = PARAM_INFECTIOUS_START_AGE + 1;
    public static final int PARAM_SYMPTOM_START_AGE = PARAM_INFECTIOUS_END_AGE + 1;
    public static final int PARAM_SYMPTOM_END_AGE = PARAM_SYMPTOM_START_AGE + 1;
    public static final int PARAM_IMMUMED_UNTIL_AGE = PARAM_SYMPTOM_END_AGE + 1;
    public static final int PARAM_LENGTH = PARAM_IMMUMED_UNTIL_AGE + 1;

    private final double[] DEFAULT_RO_RAW = {1.5, 3.5};
    // From Kucharski 2020, Kretzschmar 2020, James email
    private final double[] DEFAULT_LATANT_DURATION = {3, 6};
    private final double[] DEFAULT_INCUBATION_DURATION = {3, 7.2};
    private final double[] DEFAULT_INFECTIOUS_DURATION = {5.8, 10.8};
    private final double[] DEFAULT_POST_INFECTIOUS_DURATION = {0, 10};
    private final double[] DEFAULT_IMMUNE_DURATION = {Double.POSITIVE_INFINITY, 0};
    private final double[] DEFAULT_SYM_PROB = {0.5, 0.1};

    private final double[][] DEF_DIST_VAR = {
        DEFAULT_RO_RAW, DEFAULT_LATANT_DURATION,
        DEFAULT_INCUBATION_DURATION, DEFAULT_INFECTIOUS_DURATION,
        DEFAULT_POST_INFECTIOUS_DURATION, DEFAULT_IMMUNE_DURATION, DEFAULT_SYM_PROB};

    public static final int DIST_RO_RAW_INDEX = 0;
    public static final int DIST_LATENT_DUR_INDEX = DIST_RO_RAW_INDEX + 1;
    public static final int DIST_INCUBATION_DUR_INDEX = DIST_LATENT_DUR_INDEX + 1;
    public static final int DIST_INFECTIOUS_DUR_INDEX = DIST_INCUBATION_DUR_INDEX + 1;
    public static final int DIST_POST_INFECTIOUS_DUR_INDEX = DIST_INFECTIOUS_DUR_INDEX + 1;
    public static final int DIST_IMMUNE_DUR_INDEX = DIST_POST_INFECTIOUS_DUR_INDEX + 1;
    public static final int DIST_SYM_PROB_INDEX = DIST_IMMUNE_DUR_INDEX + 1;
    public static final int DIST_TOTAL = DIST_SYM_PROB_INDEX + 1;

    public COVID19_Remote_Infection(RandomGenerator RNG) {
        super(RNG);
        AbstractRealDistribution[] distributions = new AbstractRealDistribution[DIST_TOTAL];

        if (DEF_DIST_VAR[DIST_RO_RAW_INDEX][1] != 0) {
            double[] var = DEF_DIST_VAR[DIST_RO_RAW_INDEX];
            distributions[DIST_RO_RAW_INDEX] = new UniformRealDistribution(RNG, var[0], var[1]);
        }
        if (DEF_DIST_VAR[DIST_LATENT_DUR_INDEX][1] != 0) {
            double[] var = DEF_DIST_VAR[DIST_LATENT_DUR_INDEX];
            distributions[DIST_LATENT_DUR_INDEX] = new UniformRealDistribution(RNG, var[0], var[1]);
        }
        if (DEF_DIST_VAR[DIST_INCUBATION_DUR_INDEX][1] != 0) {
            distributions[DIST_INCUBATION_DUR_INDEX] = new WeibullDistribution(RNG, 3, 7.2); // Default
        }
        if (DEF_DIST_VAR[DIST_INFECTIOUS_DUR_INDEX][1] != 0) {
            /*
            // Pre 20200505
            double[] var = super.generatedGammaParam(DEF_DIST_VAR[DIST_INFECTIOUS_DUR_INDEX]);
            distributions[DIST_INFECTIOUS_DUR_INDEX] = new GammaDistribution(RNG, var[0], 1 / var[1]);
             */
            distributions[DIST_INFECTIOUS_DUR_INDEX] = new WeibullDistribution(RNG, 5.8, 10.8); // R0 = 5.0 (3.0, 7.0)            
            //distributions[DIST_INFECTIOUS_DUR_INDEX] = new WeibullDistribution(RNG, 5.376, 15.183);// R0 = 7.0 (4.0, 8.0)
           
        }
        if (DEF_DIST_VAR[DIST_POST_INFECTIOUS_DUR_INDEX][1] != 0) {
            double[] var = DEF_DIST_VAR[DIST_POST_INFECTIOUS_DUR_INDEX];
            distributions[DIST_POST_INFECTIOUS_DUR_INDEX] = new UniformRealDistribution(RNG, var[0], var[1]);
        }
        if (DEF_DIST_VAR[DIST_IMMUNE_DUR_INDEX][1] != 0) {
            double[] var = DEF_DIST_VAR[DIST_IMMUNE_DUR_INDEX];
            distributions[DIST_POST_INFECTIOUS_DUR_INDEX] = new UniformRealDistribution(RNG, var[0], var[1]);
        }
        if (DEF_DIST_VAR[DIST_SYM_PROB_INDEX][1] != 0) {
            double[] var = this.generatedBetaParam(DEFAULT_SYM_PROB);
            distributions[DIST_SYM_PROB_INDEX] = new BetaDistribution(this.getRNG(), var[0], var[1]);
        }

        super.storeDistributions(distributions, DEF_DIST_VAR);
        super.setInfectionState(INFECTION_STATE);

    }

    @Override
    public double advancesState(AbstractIndividualInterface p) {

        double[] param = getCurrentlyInfected().get(p.getId());
        if (param != null) {
            p.setTimeUntilNextStage(getInfectionIndex(), 1); // Check daily for now
            if (p.getAge() >= param[PARAM_INFECTED_UNTIL_AGE]) {
                p.getInfectionStatus()[getInfectionIndex()] = STATUS_IMMUNED;
            }
            if (p.getAge() >= param[PARAM_IMMUMED_UNTIL_AGE]) {
                p.getInfectionStatus()[getInfectionIndex()] = AbstractIndividualInterface.INFECT_S;
                getCurrentlyInfected().remove(p.getId());
            }

        }
        return 1;
    }

    @Override
    public double infecting(AbstractIndividualInterface target) {
        double[] param = new double[PARAM_LENGTH];
        double sample;

        target.getInfectionStatus()[getInfectionIndex()] = STATUS_INFECTED;
        target.setTimeUntilNextStage(getInfectionIndex(), 1); // Check daily for now

        // R0 
        sample = getRandomDistValue(DIST_RO_RAW_INDEX);
        param[PARAM_R0_INFECTED] = sample;

        // Exposure
        param[PARAM_AGE_OF_EXPOSURE] = target.getAge();

        // Latent
        sample = getRandomDistValue(DIST_LATENT_DUR_INDEX);
        param[PARAM_INFECTIOUS_START_AGE] = param[PARAM_AGE_OF_EXPOSURE] + Math.round(sample);

        // Infectious 
        sample = getRandomDistValue(DIST_INFECTIOUS_DUR_INDEX);
        param[PARAM_INFECTIOUS_END_AGE] = param[PARAM_INFECTIOUS_START_AGE] + Math.round(sample);

        // Post infectious
        sample = getRandomDistValue(DIST_POST_INFECTIOUS_DUR_INDEX);
        param[PARAM_INFECTED_UNTIL_AGE] = param[PARAM_INFECTIOUS_END_AGE] + Math.round(sample);

        // Determine if the person has symptoms when they become infectious
        double sym = getRandomDistValue(DIST_SYM_PROB_INDEX);
        boolean hasSym = sym >= 1;
        if (sym > 0) {
            hasSym = getRNG().nextDouble() < sym;
        }

        if (hasSym) {
            // Incubration                
            sample = getRandomDistValue(DIST_INCUBATION_DUR_INDEX);
            param[PARAM_SYMPTOM_START_AGE] = param[PARAM_AGE_OF_EXPOSURE] + Math.round(sample);

            // Here assume symptom persist until infectious 
            param[PARAM_SYMPTOM_END_AGE] = Math.min(param[PARAM_INFECTED_UNTIL_AGE],
                    Math.max(param[PARAM_SYMPTOM_START_AGE], param[PARAM_INFECTIOUS_END_AGE]));
        }

        // Immunity
        sample = getRandomDistValue(DIST_IMMUNE_DUR_INDEX);

        if (Double.isInfinite(sample)) {
            param[PARAM_IMMUMED_UNTIL_AGE] = Double.POSITIVE_INFINITY;

        } else {
            param[PARAM_IMMUMED_UNTIL_AGE] = param[PARAM_INFECTIOUS_END_AGE] + Math.round(sample);
        }

        getCurrentlyInfected().put(target.getId(), param);

        return param[PARAM_INFECTED_UNTIL_AGE] - target.getAge();

    }

    @Override
    public boolean isInfectious(AbstractIndividualInterface p) {
        if (isInfected(p)) {
            double[] param = getCurrentlyInfected().get(p.getId());
            return p.getAge() >= param[PARAM_INFECTIOUS_START_AGE]
                    && p.getAge() < param[PARAM_INFECTIOUS_END_AGE];
        } else {
            return false;
        }
    }

    @Override
    public boolean couldTransmissInfection(AbstractIndividualInterface src, AbstractIndividualInterface target) {
        return isInfectious(src)
                && target.getInfectionStatus()[getInfectionIndex()] == AbstractIndividualInterface.INFECT_S;
    }

    @Override
    public boolean isInfected(AbstractIndividualInterface p) {
        return p.getInfectionStatus()[getInfectionIndex()] != AbstractIndividualInterface.INFECT_S
                && p.getInfectionStatus()[getInfectionIndex()] != STATUS_IMMUNED;

    }

    @Override
    public boolean hasSymptoms(AbstractIndividualInterface p) {
        if (isInfected(p)) {
            double[] param = getCurrentlyInfected().get(p.getId());
            return p.getAge() >= param[PARAM_SYMPTOM_START_AGE]
                    && p.getAge() < param[PARAM_SYMPTOM_END_AGE];
        } else {
            return false;
        }

    }

    @Override
    public boolean setParameter(String id, Object value) {
        Matcher m;
        m = PATTERN_DIST_PARAM_INDEX.matcher(id);
        if (m.find()) {
            int idNum = Integer.parseInt(m.group(1));
            double[] distState = (double[]) value;
            setDistributionState(idNum, distState);
            return true;
        } else {
            throw new UnsupportedOperationException(getClass().getName() + ".setParameter: Param id = " + id + " not supported.");
        }
    }

    /**
     * Override the default one due to inclusion of other distribution
     */
    @Override
    protected void setDistributionState(int distId, double[] distState) {

        AbstractRealDistribution distribution = getDistribution(distId);

        if (distribution instanceof WeibullDistribution) {

            double shape = distState[0];
            double scale = distState[1];

            if (distState[0] < 0 || distState[1] < 0) {

                System.arraycopy(distState, 0, getDistributionState(distId), 0, distState.length);
                System.out.println(getClass().getName()
                        + ".setDistributionState (Weibull): Estimate Weibull parameters using mean and SD");

                double[] distStateAbs = new double[distState.length];
                for (int s = 0; s < distStateAbs.length; s++) {
                    distStateAbs[s] = Math.abs(distState[s]);
                }

                // http://www.real-statistics.com/distribution-fitting/method-of-moments/method-of-moments-weibull/
                BaseAbstractUnivariateSolver<UnivariateDifferentiableFunction> solver = new NewtonRaphsonSolver();

                UnivariateDifferentiableFunction function = new UnivariateDifferentiableFunction() {
                    @Override
                    public DerivativeStructure value(DerivativeStructure ds) throws DimensionMismatchException {
                        double beta = ds.getValue();
                        double val = value(beta);
                        switch (ds.getOrder()) {
                            case 0:
                                return new DerivativeStructure(ds.getFreeParameters(), 0, val);
                            case 1:
                                final int parameters = ds.getFreeParameters();
                                final double[] derivatives = new double[parameters + 1];
                                derivatives[0] = value(ds.getValue());
                                derivatives[1] = (- 2 * Gamma.digamma(1 + 2 / beta)
                                        + 2 * Gamma.digamma(1 + 1 / beta)) / (beta * beta);
                                return new DerivativeStructure(parameters, 1, derivatives);
                            default:
                                throw new NumberIsTooLargeException(ds.getOrder(), 1, true);
                        }

                    }

                    @Override
                    public double value(double beta) {
                        return Gamma.logGamma(1 + 2 / beta) - 2 * Gamma.logGamma(1 + 1 / beta)
                                - Math.log(distStateAbs[1] * distStateAbs[1] + distStateAbs[0] * distStateAbs[0])
                                + 2 * Math.log(distStateAbs[0]);
                    }
                };

                shape = solver.solve(10000, function, 0, 15);
                scale = distStateAbs[0] / Gamma.gamma(1 + 1 / shape);
            }

            distribution = new WeibullDistribution(getRNG(), shape, scale);
            setDistribution(distId, distribution);

        } else {
            super.setDistributionState(distId, distState);
        }

    }

    // Debug
    public static void main(String[] arg) {
        COVID19_Remote_Infection inf = new COVID19_Remote_Infection(new random.MersenneTwisterRandomGenerator(2251912970037127827l));
        inf.setDistributionState(DIST_INCUBATION_DUR_INDEX, new double[]{3, 7.2});
        double[] sampleValue = new double[10000];
        for (int i = 0; i < sampleValue.length; i++) {
            sampleValue[i] = inf.getDistribution(DIST_INCUBATION_DUR_INDEX).sample();
        }
        org.apache.commons.math3.stat.descriptive.DescriptiveStatistics des
                = new org.apache.commons.math3.stat.descriptive.DescriptiveStatistics(sampleValue);
        System.out.println(String.format("Mean = %.5f, SD = %.5f", des.getMean(), des.getStandardDeviation()));
    }

}
