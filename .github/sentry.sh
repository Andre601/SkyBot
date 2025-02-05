#!/bin/sh
#
# Skybot, a multipurpose discord bot
#      Copyright (C) 2017 - 2020  Duncan "duncte123" Sterken & Ramid "ramidzkh" Khan & Maurice R S "Sanduhr32"
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published
# by the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

curl -sL https://sentry.io/get-cli/ | bash
export SENTRY_RELEASE=$(./gradlew --no-daemon -q botVersion > tmp.txt && cat tmp.txt | grep "v:" | sed "s/v: //")
sentry-cli releases new SENTRY_RELEASE
sentry-cli releases set-commits --auto SENTRY_RELEASE
sentry-cli releases deploys SENTRY_RELEASE new -e "production"
sentry-cli releases finalize SENTRY_RELEASE
