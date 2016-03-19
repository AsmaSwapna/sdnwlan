#!/usr/bin/env bash
mvn install -DskipTests -Dcheckstyle.skip -U -T 1C || (echo;echo;echo build failed; exit 1)

#echo "Copying JSON File"
#ssh $ONOS_USER@$OC1 chmod +w /opt/onos/config/*.json >/dev/null 2>&1
#rcp src/main/resources/sdnwlan-config.json $ONOS_USER@$OC1:/opt/onos/config/

echo "Reinstalling the APP"
#onos-app $OC1 reinstall target/bcsw-apps-sdnwlan-1.0.1-SNAPSHOT.oar
onos-app $OC1 reinstall! target/bcsw-apps-sdnwlan-1.0.1-SNAPSHOT.oar

echo "Copying scripts"
ssh $ONOS_USER@$OC1 chmod +w /home/$ONOS_USER/*.py >/dev/null 2>&1
rcp scripts/*.py $ONOS_USER@$OC1:/home/$ONOS_USER/

