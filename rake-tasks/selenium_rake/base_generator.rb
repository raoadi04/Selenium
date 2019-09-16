# Moved from task-gen.rb
module SeleniumRake
  class BaseGenerator
    def create_deps_(out, args)
      file out => FileList[args[:srcs]] unless args[:srcs].nil?

      add_deps_(out, args[:srcs])
      add_deps_(out, args[:deps])
      add_deps_(out, args[:resources])

      task args[:name].to_sym => out unless args[:name] == out

      t = Rake::Task[args[:name].to_sym]
      t.deps = args[:deps]
      t.out = out
    end

    def add_deps_(task_name, srcs)
      return if srcs.nil?

      srcs.each do |src|
        if src.is_a? Symbol
          file task_name.to_sym => [src]
        elsif src.is_a? Hash
          add_deps_(task_name, src.keys)
        else
          # Fine. Assume we're dealing with a string, and create a FileList
          file task_name.to_sym => FileList[src]
        end
      end
    end
  end
end
