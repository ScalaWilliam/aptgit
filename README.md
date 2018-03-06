# aptgit
The way Git is supposed to be.

## Prelude: being independent
Numerous Git hosting solutions have prevented progress via the simple fact that data is exposed not via Git, but via HTTP APIs. And that updates are exposed via non-standardised webhooks.

This project concerns itself with data down to Git level. What sort of data? Take a look at the typical day-to-day workflow: issue tracker, repository hosting, continuous integration and finally deployment. All of this data belogs directly in Git rather than outside of it.

The reason is simple: it's all coupled, and the principle is: don't couple that which shouldn't be coupled; don't decouple stuff that which should be coupled.

I've faced countless problems with this coupled information being torn apart, where I'm unable to exercise the full power of Git and design tightly-wrapped workflows. The very first of these problems I faced back 12 years ago was continuously deploying website content: in order to do it, I'd need to open up a custom script that accepts webhooks, verify that I'm not being DOSsed, set up the webhook, and hope it works. It was always cumbersome but I didn't know what to do about it other than push to self-managed Git repository which would lose me all the benefits of a Git repository.

Once I began working in larger teams, I came across issue systems. In particular, JIRA, and later on, GitHub. So powerful, yet so... hard to analyse: dozens of HTTP endpoints, authentications and parsing magic to happen. Why can't I re-use my Git authentication? Why can't I simply access this as data, locally? Why do I have to be worried about whether GitHub will one day decide to remove my project, and thus kill its whole Issue history? I shouldn't be. Just like I'm not worried about the code as it's cloned locally and in multiple places, why can't the issues be as well?

--

Later on in my career, especially 2016 onwards, I kept facing this annoying thing: having to enforce rules in development workflows by *telling* and *ordering* people around. I don't like to order people around. So if I want somebody to get something done, I need a manager to help.

However, if we have forms that prevent you from entering a phone number as an e-mail address, why cannot we do the same for development workflows? One very simple situation I faced with a operations team was that they wanted at least 2 approvals on a GitHub pull request. The rules were not written down anywhere, and we pushing through changes into their repository without them even realising until later, and then scolding the development team. But why cannot this just be prevented by software?

Suddenly I came to the conclusion that no matter how many featuers GitHub will release, they will never achieve what each specific team wants. The reason is simple: GitHub is the controller, not the team. GitHub manages the code and thus is the bottleneck, and will forever be. And by now you'll know how I despise bottlenecks. I dislike to every time have to enforce rules: formatting, file naming, number of files changed, proper commit descriptions, appropriate number of reviewers... and so forth. Why can't the computer do it for me?

## Building blocks: reactivity with WebSub

Reactivity over HTTP has not been standardised until quite recently: 23 January 2018. It's in WebSub protocol: https://www.w3.org/TR/websub/

WebSub is just webhooks-as-a-standard. Key principle is that to be able to subscribe to events on demand, in a standard way. It's perfect for low-frequency events, ie Git updates.

## Un-paining continuous deployment/integration

I couldn't take it any more. Why do I need to manage a Jenkins instance? Why do I need to give deployment credentials to travis-ci? This is where https://git.watch/ was born. Serverless webhooks. Simply run a CLI client that executes a custom command every time your repository is updated. Let your script decide what to do with it. Let your client decide what happens, not the server. Now you can test your auto-deployments without having to make those commits called "Test commit" :-).

I didn't understand the point of the whole redundancy in reading changesets and finding deltas via JSON payloads: the data is already there in Git!!

With git-watch, I can literally do: `git-watch -i 'git pull origin && git push somewhere-else'` to sync a Git repository. Wow so simple.

## Un-paining issue management

As I develop incrementally, I like to get as much as possible unblocked and then fill the rest of gaps later. I came up with **gitchiu** which creates GitHub Issues from Git commit descriptions. It was really fun as I could specify "bugs" immediately as I create them, even before pushing my code.

## Building blocks: git-notes and git-appraise

git-appraise, by Google, allows distributed code reviews: https://github.com/google/git-appraise
Again, why should it be centralised at all?

git-appraise utilises git-notes: https://git-scm.com/docs/git-notes

git-notes also has a Jenkins integration: https://github.com/jenkinsci/google-git-notes-publisher-plugin

Fun, isn't it, now that you could have all this data available without any funky APIs?

# Defining this

We define a minimal system of reacting to Git updates. With this reactivity, we can instantly build Issue tracking systems, continuous integration systems, continuous deployment systems, all without coupling and with proper inversion of control.

So, what is the minimum? Well, it is:
- HTTP endpoint that contains "last updated" date, WebSub tags and `link` tags to the relevant Git repository.
- Git Server Hook that sends updates to the corresponding WebSub location and changes the "last updated" date.

It's pretty much independent of the Git server implementation, and could even be implemented By GitHub, GitLab and others. Whether they will, is another question. GitHub one day decided to remove git notes. They left a message saying it's no longer supported, refusing to make any indication of why: https://blog.github.com/2010-08-25-git-notes-display/

I intend to build a whole ecosystem around this as I like lightweight things. Most exciting to me is lightweight, framework-free continuous integrations and deployments, in spirit of Git Watch.

Next is Issue tracking that is coupled with Pull requests (or Change Request). Next is enhancing my lovely "Git Work" workflows: see more @ http://git.work/

Good night, chaps.
