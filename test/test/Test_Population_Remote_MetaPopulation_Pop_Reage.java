package test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import person.AbstractIndividualInterface;
import person.Person_Remote_MetaPopulation;
import population.Population_Remote_MetaPopulation;
import random.MersenneTwisterRandomGenerator;
import util.FileZipper;

/**
 *
 * @author Bhui
 */
public class Test_Population_Remote_MetaPopulation_Pop_Reage {

    public static void main(String[] arg) throws IOException, ClassNotFoundException {
        File importPopPath = new File("C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\ImportDir");
        File outputPopPath = new File("C:\\Users\\Bhui\\OneDrive - UNSW\\RMP\\ImportDirReage");
        int startReage = 5841;
        int reageRange = 360;

        random.RandomGenerator rng = new MersenneTwisterRandomGenerator(2251912970037127827l);

        outputPopPath.mkdirs();
        Pattern Pattern_importFile = Pattern.compile("pop_S(\\d+).zip");

        File[] popFiles = importPopPath.listFiles();

        for (File popOrg : popFiles) {
            Population_Remote_MetaPopulation pop;

            File tempPop = FileZipper.unzipFile(popOrg, importPopPath);
            try (ObjectInputStream oIStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(tempPop)))) {
                pop = Population_Remote_MetaPopulation.decodeFromStream(oIStream);
            }
            tempPop.delete();

            Matcher m = Pattern_importFile.matcher(popOrg.getName());
            m.find();
            int sId = Integer.parseInt(m.group(1));

            System.out.println("Reaging pop file: " + popOrg.getAbsolutePath() + " with sId = " + sId);

            AbstractIndividualInterface[] persons = pop.getPop();
            for (AbstractIndividualInterface p : persons) {
                if (p.getAge() >= startReage) {
                    int diff = ((int) p.getAge()) - startReage;
                    int div = diff / reageRange;
                    int rem = diff - div * reageRange;
                    int newAge = div * reageRange + rng.nextInt(reageRange) + startReage;
                    int ageDiff = (int) (newAge - p.getAge());

                    p.setAge(newAge);

                    if (p instanceof Person_Remote_MetaPopulation) {
                        Person_Remote_MetaPopulation rmp = (Person_Remote_MetaPopulation) p;
                        rmp.getFields()[Person_Remote_MetaPopulation.PERSON_FIRST_SEEK_PARTNER_AGE]
                                += ageDiff;
                        rmp.getFields()[Person_Remote_MetaPopulation.PERSON_FIRST_SEX_AGE]
                                += ageDiff;

                        int numInf = rmp.getInfectionStatus().length;
                        for (int i = 0; i < numInf; i++) {
                            if (rmp.getLastInfectedAtAge(i) >= 0) {
                                rmp.setLastInfectedAtAge(i, rmp.getLastInfectedAtAge(i) + ageDiff);
                            }
                            if (rmp.getLastTreatedAt(i) >= 0) {
                                rmp.setLastTreatedAt(i, rmp.getLastTreatedAt(i) + ageDiff);
                            }

                        }

                        for (int i = 0; i < rmp.getPartnerHistoryLifetimePt(); i++) {
                            rmp.getPartnerHistoryLifetimeAtAge()[i] += ageDiff;
                        }

                    }

                }
            }

            String popName = "pop_S" + sId;

            File tempFile = new File(outputPopPath, popName);
            File outputFile = new File(outputPopPath, popOrg.getName());

            try (ObjectOutputStream outStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)))) {
                pop.encodePopToStream(outStream);
            }
            FileZipper.zipFile(tempFile, outputFile);
            tempFile.delete();

            System.out.println("Reage pop file at " + outputFile.getAbsolutePath());

        }

    }

}
