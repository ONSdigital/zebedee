#!/bin/bash

ROOT_DIRECTORY="`pwd`"
FLORENCE_DIRECTORY="$ROOT_DIRECTORY/florence-user-test"
ZEBEDEE_DIRECTORY="$ROOT_DIRECTORY/zebedee-user-test"
TREDEGAR_DIRECTORY="$ROOT_DIRECTORY/tredegar-user-test"
MR_RUSTY_DIRECTORY="$ROOT_DIRECTORY/mr-rusty-user-test"

function update_branch {
    #echo "Updating $3 branch of $1 into $2"
    # if the directory does no exist we need to clone the repo.
    if [ ! -e $2 ]; then
        git clone $1 $2
    fi

    # get the latest for the given branch
    cd $2
    git checkout $3
    git pull --rebase
}

function send_slack_message {
    echo $1
    #curl --data "$1" $'https://onsbeta.slack.com/services/hooks/slackbot?token=ZhoG2gy2TTTBMhtODncuprDQ&channel=%23github'
}

function run_tests {
    # build the tests
    mvn package > /dev/null
    echo "Running tests..."

    # run the tests, and add the output to the output variable
    if ! java $JAVA_OPTS -cp 'target/*' com.github.onsdigital.TestRunner; then
        send_slack_message "Automation tests are failing after zebedee commit"
    else
        send_slack_message "Automation tests passed after zebedee commit"
    fi
}


# update all projects concurrently with & wait
update_branch https://github.com/ONSdigital/florence.git $FLORENCE_DIRECTORY develop &
update_branch https://github.com/Carboni/zebedee.git $ZEBEDEE_DIRECTORY develop
update_branch https://github.com/ONSdigital/tredegar.git $TREDEGAR_DIRECTORY develop
wait


$FLORENCE_DIRECTORY/run.sh > /dev/null &
$ZEBEDEE_DIRECTORY/files.sh > /dev/null &
$TREDEGAR_DIRECTORY/run.sh > /dev/null &
(
    update_branch https://github.com/Carboni/MrRusty.git $MR_RUSTY_DIRECTORY master
    cd "$MR_RUSTY_DIRECTORY"
    run_tests
)
wait





